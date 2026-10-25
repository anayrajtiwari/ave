//! SoundCraft Pro Audio Workstation & DSP Integration for AVE.
//!
//! Exposes SoundCraft's studio-grade DSP processors (7-band parametric EQ,
//! compressor/limiter, room reverb, tape saturation) and metering tools directly to
//! the Android application via UniFFI.

use soundcraft_dsp::meter::PeakMeter;
use soundcraft_dsp::{create, plugins};

/// Studio mastering presets for audio sweetening.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum SoundcraftPreset {
    Neutral,
    VocalClarity,
    WarmTape,
    PodcastMaster,
    BassPunch,
    SpaciousAir,
}

/// Comprehensive parameter set for the SoundCraft Studio DSP chain.
#[derive(Debug, Clone, uniffi::Record)]
pub struct SoundcraftDSPParams {
    // 7-Band Parametric EQ
    pub eq_enabled: bool,
    pub input_gain_db: f32,
    pub hpf_freq_hz: f32,
    pub low_shelf_gain_db: f32,
    pub low_mid_gain_db: f32,
    pub mid_gain_db: f32,
    pub high_mid_gain_db: f32,
    pub high_shelf_gain_db: f32,
    pub eq_output_gain_db: f32,

    // Compressor / Dynamics
    pub comp_enabled: bool,
    pub comp_threshold_db: f32,
    pub comp_ratio: f32,
    pub comp_attack_ms: f32,
    pub comp_release_ms: f32,
    pub comp_makeup_db: f32,

    // Algorithmic Reverb
    pub reverb_enabled: bool,
    pub reverb_mix_percent: f32,
    pub reverb_size_percent: f32,
    pub reverb_decay_secs: f32,
    pub reverb_damping_hz: f32,

    // Tape / Tube Saturation
    pub sat_enabled: bool,
    pub sat_drive_db: f32,
    pub sat_mix_percent: f32,
}

/// Audio metering metrics for levels, loudness, and dynamic headroom.
#[derive(Debug, Clone, uniffi::Record)]
pub struct SoundcraftMeters {
    pub peak_db: f32,
    pub rms_db: f32,
    pub dynamic_range_db: f32,
}

/// Returns standard default parameters for a clean, unprocessed pass.
#[uniffi::export]
pub fn soundcraft_default_params() -> SoundcraftDSPParams {
    SoundcraftDSPParams {
        eq_enabled: false,
        input_gain_db: 0.0,
        hpf_freq_hz: 20.0,
        low_shelf_gain_db: 0.0,
        low_mid_gain_db: 0.0,
        mid_gain_db: 0.0,
        high_mid_gain_db: 0.0,
        high_shelf_gain_db: 0.0,
        eq_output_gain_db: 0.0,

        comp_enabled: false,
        comp_threshold_db: -20.0,
        comp_ratio: 4.0,
        comp_attack_ms: 10.0,
        comp_release_ms: 100.0,
        comp_makeup_db: 0.0,

        reverb_enabled: false,
        reverb_mix_percent: 25.0,
        reverb_size_percent: 50.0,
        reverb_decay_secs: 1.2,
        reverb_damping_hz: 6000.0,

        sat_enabled: false,
        sat_drive_db: 6.0,
        sat_mix_percent: 50.0,
    }
}

/// Returns fine-tuned studio DSP parameters according to the selected SoundCraft preset.
#[uniffi::export]
pub fn soundcraft_preset_params(preset: SoundcraftPreset) -> SoundcraftDSPParams {
    let mut p = soundcraft_default_params();
    match preset {
        SoundcraftPreset::Neutral => p,
        SoundcraftPreset::VocalClarity => {
            p.eq_enabled = true;
            p.hpf_freq_hz = 80.0;
            p.low_shelf_gain_db = -2.0;
            p.low_mid_gain_db = -3.0; // cut mud
            p.mid_gain_db = 1.0;
            p.high_mid_gain_db = 3.5; // presence
            p.high_shelf_gain_db = 2.0; // air

            p.comp_enabled = true;
            p.comp_threshold_db = -18.0;
            p.comp_ratio = 3.0;
            p.comp_attack_ms = 15.0;
            p.comp_release_ms = 80.0;
            p.comp_makeup_db = 2.0;
            p
        }
        SoundcraftPreset::WarmTape => {
            p.eq_enabled = true;
            p.low_shelf_gain_db = 1.5;
            p.high_shelf_gain_db = -1.0;

            p.comp_enabled = true;
            p.comp_threshold_db = -15.0;
            p.comp_ratio = 2.0;
            p.comp_attack_ms = 25.0;
            p.comp_release_ms = 120.0;
            p.comp_makeup_db = 1.0;

            p.sat_enabled = true;
            p.sat_drive_db = 8.0;
            p.sat_mix_percent = 40.0;
            p
        }
        SoundcraftPreset::PodcastMaster => {
            p.eq_enabled = true;
            p.hpf_freq_hz = 75.0;
            p.low_shelf_gain_db = -1.0;
            p.low_mid_gain_db = -2.5;
            p.high_mid_gain_db = 2.5;
            p.high_shelf_gain_db = 1.5;

            p.comp_enabled = true;
            p.comp_threshold_db = -22.0;
            p.comp_ratio = 4.0;
            p.comp_attack_ms = 8.0;
            p.comp_release_ms = 90.0;
            p.comp_makeup_db = 4.0;
            p
        }
        SoundcraftPreset::BassPunch => {
            p.eq_enabled = true;
            p.low_shelf_gain_db = 4.5;
            p.low_mid_gain_db = 2.0;
            p.high_shelf_gain_db = 1.0;

            p.comp_enabled = true;
            p.comp_threshold_db = -20.0;
            p.comp_ratio = 5.0;
            p.comp_attack_ms = 30.0;
            p.comp_release_ms = 60.0;
            p.comp_makeup_db = 3.0;
            p
        }
        SoundcraftPreset::SpaciousAir => {
            p.eq_enabled = true;
            p.high_mid_gain_db = 2.0;
            p.high_shelf_gain_db = 4.0;

            p.reverb_enabled = true;
            p.reverb_mix_percent = 35.0;
            p.reverb_size_percent = 70.0;
            p.reverb_decay_secs = 2.0;
            p.reverb_damping_hz = 8000.0;
            p
        }
    }
}

/// Processes an interleaved audio buffer through the SoundCraft DSP studio pipeline.
#[uniffi::export]
pub fn soundcraft_process_audio_buffer(
    samples: Vec<f32>,
    channels: u32,
    sample_rate: u32,
    params: SoundcraftDSPParams,
) -> Vec<f32> {
    if samples.is_empty() || channels == 0 || sample_rate == 0 {
        return samples;
    }

    let ch = channels as usize;
    let frames = samples.len() / ch;
    if frames == 0 {
        return samples;
    }

    let sr = sample_rate as f32;

    // De-interleave into planar channel buffers
    let mut planar: Vec<Vec<f32>> = (0..ch).map(|_| Vec::with_capacity(frames)).collect();
    for chunk in samples.chunks_exact(ch) {
        for (c, &s) in chunk.iter().enumerate() {
            planar[c].push(s);
        }
    }

    // 1. Parametric EQ
    if params.eq_enabled {
        if let Some(mut eq) = create("eq_7band") {
            eq.prepare(sr, frames, ch);
            eq.set_param("input_gain", params.input_gain_db);
            eq.set_param("hpf_on", if params.hpf_freq_hz > 20.0 { 1.0 } else { 0.0 });
            eq.set_param("hpf_freq", params.hpf_freq_hz);
            eq.set_param("low_shelf_gain", params.low_shelf_gain_db);
            eq.set_param("low_mid_gain", params.low_mid_gain_db);
            eq.set_param("mid_gain", params.mid_gain_db);
            eq.set_param("high_mid_gain", params.high_mid_gain_db);
            eq.set_param("high_shelf_gain", params.high_shelf_gain_db);
            eq.set_param("output_gain", params.eq_output_gain_db);
            eq.process(&mut planar, frames);
        }
    }

    // 2. Dynamics / Compressor
    if params.comp_enabled {
        if let Some(mut comp) = create("compressor") {
            comp.prepare(sr, frames, ch);
            comp.set_param("threshold", params.comp_threshold_db);
            comp.set_param("ratio", params.comp_ratio);
            comp.set_param("attack", params.comp_attack_ms);
            comp.set_param("release", params.comp_release_ms);
            comp.set_param("makeup", params.comp_makeup_db);
            comp.process(&mut planar, frames);
        }
    }

    // 3. Tape / Tube Saturation
    if params.sat_enabled {
        if let Some(mut sat) = create("saturator") {
            sat.prepare(sr, frames, ch);
            sat.set_param("drive", params.sat_drive_db);
            sat.set_param("mix", params.sat_mix_percent);
            sat.process(&mut planar, frames);
        }
    }

    // 4. Room Reverb
    if params.reverb_enabled {
        if let Some(mut rev) = create("room_reverb") {
            rev.prepare(sr, frames, ch);
            rev.set_param("mix", params.reverb_mix_percent);
            rev.set_param("size", params.reverb_size_percent);
            rev.set_param("decay", params.reverb_decay_secs);
            rev.set_param("damping", params.reverb_damping_hz);
            rev.process(&mut planar, frames);
        }
    }

    // Re-interleave back to interleaved samples
    let mut out = Vec::with_capacity(samples.len());
    for f in 0..frames {
        for c in 0..ch {
            out.push(planar[c][f]);
        }
    }
    out
}

/// Analyzes an audio buffer to calculate peak dB, RMS dB, and dynamic range.
#[uniffi::export]
pub fn soundcraft_analyze_audio(
    samples: Vec<f32>,
    _channels: u32,
    sample_rate: u32,
) -> SoundcraftMeters {
    if samples.is_empty() || sample_rate == 0 {
        return SoundcraftMeters { peak_db: -96.0, rms_db: -96.0, dynamic_range_db: 0.0 };
    }

    let mut meter = PeakMeter::new(sample_rate as f32);
    meter.process(&samples);
    let peak_db = meter.level_db();

    let sum_sq: f32 = samples.iter().map(|&s| s * s).sum();
    let mean_sq = sum_sq / (samples.len() as f32);
    let rms_linear = mean_sq.sqrt();
    let rms_db = if rms_linear > 1e-6 {
        20.0 * rms_linear.log10()
    } else {
        -96.0
    };

    let dynamic_range_db = (peak_db - rms_db).max(0.0);

    SoundcraftMeters {
        peak_db,
        rms_db,
        dynamic_range_db,
    }
}

/// Returns a list of all audio processing plugins supported in SoundCraft.
#[uniffi::export]
pub fn soundcraft_available_plugins() -> Vec<String> {
    plugins().iter().map(|p| format!("{} ({})", p.name, p.id)).collect()
}
