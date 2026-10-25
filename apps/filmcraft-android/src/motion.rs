//! Motion, animations, and keyframes for video, images, text, and graphics.

use filmcraft_engine::{EngineError, Session};
use filmcraft_geom::Vec2;
use filmcraft_project::{ClipId, Keyframe, ParamValue, TrackKind};
use filmcraft_time::Tick;

use crate::{MobileError, edits, model};

#[derive(Debug, Clone, uniffi::Record)]
pub struct KeyframePoint {
    pub time_ms: i64,
    pub scale: f64,
    pub rotation: f64,
    pub x: f64,
    pub y: f64,
    pub opacity: f64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct MotionInfo {
    pub scale: f64,
    pub rotation: f64,
    pub x: f64,
    pub y: f64,
    pub opacity: f64,
    pub keyframes: Vec<KeyframePoint>,
}

fn other(msg: impl Into<String>) -> EngineError {
    EngineError::Other(msg.into())
}

fn locate_video_clip<'a>(s: &'a Session, id: u64) -> Result<(f64, f64, &'a filmcraft_project::TrackItem), MobileError> {
    let (_, seq) = model::sequence(s)?;
    let (fw, fh) = (f64::from(seq.settings.width), f64::from(seq.settings.height));
    let f = model::find(seq, ClipId(id)).filter(|f| f.kind == TrackKind::Video).ok_or_else(|| MobileError::Command("That clip has no picture.".into()))?;
    Ok((fw, fh, f.clip))
}

/// Query motion attributes and existing keyframes of a clip.
pub(crate) fn motion_info(s: &Session, id: u64) -> Result<MotionInfo, MobileError> {
    let (fw, fh, c) = locate_video_clip(s, id)?;
    let mt = c.source_in;
    let (mut x, mut y, mut scale, mut rotation) = (fw / 2.0, fh / 2.0, 100.0, 0.0);
    let mut opacity = 100.0;

    let mut points: Vec<KeyframePoint> = Vec::new();

    if let Some(m) = c.effects.iter().find(|e| e.effect == "motion" && e.enabled) {
        let p = m.vec2_at("position", mt);
        if !p.x.is_nan() && !p.y.is_nan() {
            (x, y) = (p.x, p.y);
        }
        scale = m.f64_at("scale", mt);
        rotation = m.f64_at("rotation", mt);

        // collect keyframes from scale, rotation, and position
        let mut times: Vec<Tick> = Vec::new();
        if let Some(param) = m.param("scale") {
            for k in &param.keyframes {
                times.push(k.time);
            }
        }
        if let Some(param) = m.param("rotation") {
            for k in &param.keyframes {
                times.push(k.time);
            }
        }
        if let Some(param) = m.param("position") {
            for k in &param.keyframes {
                times.push(k.time);
            }
        }

        times.sort_by_key(|t| t.0);
        times.dedup();

        for t in times {
            let p_at = m.vec2_at("position", t);
            let kx = if p_at.x.is_nan() { x } else { p_at.x };
            let ky = if p_at.y.is_nan() { y } else { p_at.y };
            let ks = m.f64_at("scale", t);
            let kr = m.f64_at("rotation", t);

            let time_offset = (t - c.source_in).seconds();
            let time_ms = (time_offset * 1000.0).round() as i64;

            points.push(KeyframePoint {
                time_ms,
                scale: ks,
                rotation: kr,
                x: kx,
                y: ky,
                opacity: 100.0,
            });
        }
    }

    if let Some(op) = c.effects.iter().find(|e| e.effect == "opacity" && e.enabled) {
        opacity = op.f64_at("opacity", mt);
    }

    Ok(MotionInfo {
        scale,
        rotation,
        x,
        y,
        opacity,
        keyframes: points,
    })
}

/// Reset or clear motion and opacity keyframes back to static state.
pub(crate) fn clear_motion(s: &mut Session, id: u64) -> Result<(), MobileError> {
    let (fw, fh, _) = locate_video_clip(s, id)?;
    s.edit_sequence("Clear Motion", |q, _, _| {
        let c = q.find_item_mut(ClipId(id)).map(|x| x.1).ok_or_else(|| other("clip gone"))?;
        if let Some(m) = c.effects.iter_mut().find(|e| e.effect == "motion") {
            if let Some(p) = m.param_mut("scale") {
                p.keyframes.clear();
                p.value = ParamValue::Float(100.0);
            }
            if let Some(p) = m.param_mut("rotation") {
                p.keyframes.clear();
                p.value = ParamValue::Float(0.0);
            }
            if let Some(p) = m.param_mut("position") {
                p.keyframes.clear();
                p.value = ParamValue::Vec2(Vec2::new(fw / 2.0, fh / 2.0));
            }
        }
        if let Some(o) = c.effects.iter_mut().find(|e| e.effect == "opacity") {
            if let Some(p) = o.param_mut("opacity") {
                p.keyframes.clear();
                p.value = ParamValue::Float(100.0);
            }
        }
        Ok(())
    })?;
    edits::settle(s);
    Ok(())
}

/// Set a motion animation preset on clip `id`.
pub(crate) fn set_motion_preset(s: &mut Session, id: u64, preset: &str, duration_sec: f64) -> Result<(), MobileError> {
    let (fw, fh, item_info) = locate_video_clip(s, id)?;
    let clip_dur_sec = item_info.duration.seconds();
    let anim_sec = duration_sec.clamp(0.1, clip_dur_sec.max(0.1));
    let t0 = item_info.source_in;
    let t_end = t0 + item_info.duration;
    let cx = fw / 2.0;
    let cy = fh / 2.0;

    let kf_f = |t: Tick, v: f64| Keyframe::new(t, ParamValue::Float(v));
    let kf_pos = |t: Tick, x: f64, y: f64| Keyframe::new(t, ParamValue::Vec2(Vec2::new(x, y)));

    s.edit_sequence("Motion Preset", |q, _, _| {
        let c = q.find_item_mut(ClipId(id)).map(|x| x.1).ok_or_else(|| other("clip gone"))?;
        // reset keyframes and apply preset
        if let Some(m) = c.effects.iter_mut().find(|e| e.effect == "motion") {
            if let Some(p) = m.param_mut("scale") { p.keyframes.clear(); p.value = ParamValue::Float(100.0); }
            if let Some(p) = m.param_mut("rotation") { p.keyframes.clear(); p.value = ParamValue::Float(0.0); }
            if let Some(p) = m.param_mut("position") { p.keyframes.clear(); p.value = ParamValue::Vec2(Vec2::new(cx, cy)); }

            match preset {
                "slide_left" => {
                    if let Some(p) = m.param_mut("position") {
                        p.keyframes = vec![
                            kf_pos(t0, fw * 1.5, cy),
                            kf_pos(t0 + Tick::from_seconds_f64(anim_sec), cx, cy),
                        ];
                    }
                }
                "slide_right" => {
                    if let Some(p) = m.param_mut("position") {
                        p.keyframes = vec![
                            kf_pos(t0, -fw * 0.5, cy),
                            kf_pos(t0 + Tick::from_seconds_f64(anim_sec), cx, cy),
                        ];
                    }
                }
                "slide_up" => {
                    if let Some(p) = m.param_mut("position") {
                        p.keyframes = vec![
                            kf_pos(t0, cx, fh * 1.5),
                            kf_pos(t0 + Tick::from_seconds_f64(anim_sec), cx, cy),
                        ];
                    }
                }
                "slide_down" => {
                    if let Some(p) = m.param_mut("position") {
                        p.keyframes = vec![
                            kf_pos(t0, cx, -fh * 0.5),
                            kf_pos(t0 + Tick::from_seconds_f64(anim_sec), cx, cy),
                        ];
                    }
                }
                "pop" => {
                    if let Some(p) = m.param_mut("scale") {
                        let half = anim_sec * 0.65;
                        p.keyframes = vec![
                            kf_f(t0, 0.0),
                            kf_f(t0 + Tick::from_seconds_f64(half), 115.0),
                            kf_f(t0 + Tick::from_seconds_f64(anim_sec), 100.0),
                        ];
                    }
                }
                "zoom_in" => {
                    if let Some(p) = m.param_mut("scale") {
                        p.keyframes = vec![
                            kf_f(t0, 50.0),
                            kf_f(t0 + Tick::from_seconds_f64(anim_sec), 100.0),
                        ];
                    }
                }
                "zoom_out" => {
                    if let Some(p) = m.param_mut("scale") {
                        p.keyframes = vec![
                            kf_f(t0, 160.0),
                            kf_f(t0 + Tick::from_seconds_f64(anim_sec), 100.0),
                        ];
                    }
                }
                "ken_burns" => {
                    if let Some(p) = m.param_mut("scale") {
                        p.keyframes = vec![
                            kf_f(t0, 100.0),
                            kf_f(t_end, 120.0),
                        ];
                    }
                    if let Some(p) = m.param_mut("position") {
                        p.keyframes = vec![
                            kf_pos(t0, cx, cy),
                            kf_pos(t_end, cx * 1.05, cy * 0.96),
                        ];
                    }
                }
                "pulse" => {
                    if let Some(p) = m.param_mut("scale") {
                        let d = anim_sec / 4.0;
                        p.keyframes = vec![
                            kf_f(t0, 100.0),
                            kf_f(t0 + Tick::from_seconds_f64(d), 112.0),
                            kf_f(t0 + Tick::from_seconds_f64(d * 2.0), 100.0),
                            kf_f(t0 + Tick::from_seconds_f64(d * 3.0), 112.0),
                            kf_f(t0 + Tick::from_seconds_f64(d * 4.0), 100.0),
                        ];
                    }
                }
                "shake" => {
                    if let Some(p) = m.param_mut("position") {
                        let step = anim_sec / 6.0;
                        p.keyframes = vec![
                            kf_pos(t0, cx, cy),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 1.0), cx - 12.0, cy + 8.0),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 2.0), cx + 14.0, cy - 6.0),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 3.0), cx - 10.0, cy - 8.0),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 4.0), cx + 8.0, cy + 10.0),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 5.0), cx - 4.0, cy - 2.0),
                            kf_pos(t0 + Tick::from_seconds_f64(step * 6.0), cx, cy),
                        ];
                    }
                }
                _ => {}
            }
            m.enabled = true;
        }

        if let Some(o) = c.effects.iter_mut().find(|e| e.effect == "opacity") {
            if let Some(p) = o.param_mut("opacity") {
                p.keyframes.clear();
                p.value = ParamValue::Float(100.0);
            }
            match preset {
                "fade_in" => {
                    if let Some(p) = o.param_mut("opacity") {
                        p.keyframes = vec![
                            kf_f(t0, 0.0),
                            kf_f(t0 + Tick::from_seconds_f64(anim_sec), 100.0),
                        ];
                    }
                }
                "fade_out" => {
                    if let Some(p) = o.param_mut("opacity") {
                        let start_fade = t_end - Tick::from_seconds_f64(anim_sec);
                        p.keyframes = vec![
                            kf_f(start_fade.max(t0), 100.0),
                            kf_f(t_end, 0.0),
                        ];
                    }
                }
                _ => {}
            }
            o.enabled = true;
        }
        Ok(())
    })?;
    edits::settle(s);
    Ok(())
}

/// Add or update a keyframe at `time_offset_ms` from clip start.
pub(crate) fn add_motion_keyframe(
    s: &mut Session,
    id: u64,
    time_offset_ms: i64,
    scale: Option<f64>,
    rotation: Option<f64>,
    x: Option<f64>,
    y: Option<f64>,
    opacity: Option<f64>,
) -> Result<(), MobileError> {
    let (_, _, item_info) = locate_video_clip(s, id)?;
    let target_time = item_info.source_in + Tick::from_seconds_f64(time_offset_ms as f64 / 1000.0);

    s.edit_sequence("Add Keyframe", |q, _, _| {
        let c = q.find_item_mut(ClipId(id)).map(|x| x.1).ok_or_else(|| other("clip gone"))?;

        if let Some(m) = c.effects.iter_mut().find(|e| e.effect == "motion") {
            if let Some(s_val) = scale {
                if let Some(p) = m.param_mut("scale") {
                    p.keyframes.retain(|k| (k.time.0 - target_time.0).abs() > (Tick::from_seconds_f64(0.04).0));
                    p.keyframes.push(Keyframe::new(target_time, ParamValue::Float(s_val)));
                    p.keyframes.sort_by_key(|k| k.time.0);
                }
            }

            if let Some(r_val) = rotation {
                if let Some(p) = m.param_mut("rotation") {
                    p.keyframes.retain(|k| (k.time.0 - target_time.0).abs() > (Tick::from_seconds_f64(0.04).0));
                    p.keyframes.push(Keyframe::new(target_time, ParamValue::Float(r_val)));
                    p.keyframes.sort_by_key(|k| k.time.0);
                }
            }

            if let (Some(px), Some(py)) = (x, y) {
                if let Some(p) = m.param_mut("position") {
                    p.keyframes.retain(|k| (k.time.0 - target_time.0).abs() > (Tick::from_seconds_f64(0.04).0));
                    p.keyframes.push(Keyframe::new(target_time, ParamValue::Vec2(Vec2::new(px, py))));
                    p.keyframes.sort_by_key(|k| k.time.0);
                }
            }
            m.enabled = true;
        }

        if let Some(op_val) = opacity {
            if let Some(o) = c.effects.iter_mut().find(|e| e.effect == "opacity") {
                if let Some(p) = o.param_mut("opacity") {
                    p.keyframes.retain(|k| (k.time.0 - target_time.0).abs() > (Tick::from_seconds_f64(0.04).0));
                    p.keyframes.push(Keyframe::new(target_time, ParamValue::Float(op_val)));
                    p.keyframes.sort_by_key(|k| k.time.0);
                }
                o.enabled = true;
            }
        }
        Ok(())
    })?;
    edits::settle(s);
    Ok(())
}
