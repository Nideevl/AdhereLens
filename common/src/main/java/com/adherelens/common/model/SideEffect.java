package com.adherelens.common.model;

/**
 * The fixed list of side effects a doctor or nurse can record. Coded values only, no free
 * text, so the same effect is always spelled the same way for analytics. OTHER is for an
 * effect that is not on the list.
 */
public enum SideEffect {
    NAUSEA,
    VOMITING,
    DIARRHEA,
    CONSTIPATION,
    ABDOMINAL_PAIN,
    HEARTBURN,
    LOSS_OF_APPETITE,
    DRY_MOUTH,
    HEADACHE,
    DIZZINESS,
    DROWSINESS,
    FATIGUE,
    INSOMNIA,
    TREMOR,
    CONFUSION,
    BLURRED_VISION,
    RASH,
    ITCHING,
    SWELLING,
    MUSCLE_PAIN,
    JOINT_PAIN,
    COUGH,
    SHORTNESS_OF_BREATH,
    PALPITATIONS,
    LOW_BLOOD_PRESSURE,
    HIGH_BLOOD_PRESSURE,
    FEVER,
    WEIGHT_GAIN,
    WEIGHT_LOSS,
    OTHER
}
