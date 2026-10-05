package com.naresh.lungsdemo.model

enum class AuscultationLocation(
    val id: Int,
    val displayName: String
) {
    ANTERIOR_RIGHT_APEX(
        1,
        "Right Supraclavicular / Apex"
    ),

    ANTERIOR_LEFT_APEX(
        2,
        "Left Supraclavicular / Apex"
    ),

    ANTERIOR_RIGHT_UPPER(
        3,
        "Right Upper Chest"
    ),

    ANTERIOR_LEFT_UPPER(
        4,
        "Left Upper Chest"
    ),

    ANTERIOR_RIGHT_MIDDLE(
        5,
        "Right Middle Chest"
    ),

    ANTERIOR_LEFT_MIDDLE(
        6,
        "Left Middle Chest"
    ),

    ANTERIOR_RIGHT_LOWER(
        7,
        "Right Lower Anterior/Lateral Chest"
    ),

    ANTERIOR_LEFT_LOWER(
        8,
        "Left Lower Anterior/Lateral Chest"
    ),

    POSTERIOR_RIGHT_UPPER(
        9,
        "Right Upper Back"
    ),

    POSTERIOR_LEFT_UPPER(
        10,
        "Left Upper Back"
    ),

    POSTERIOR_RIGHT_INTERSCAPULAR(
        11,
        "Right Interscapular Area"
    ),

    POSTERIOR_LEFT_INTERSCAPULAR(
        12,
        "Left Interscapular Area"
    ),

    POSTERIOR_RIGHT_LOWER(
        13,
        "Right Lower Lung Field / Base"
    ),

    POSTERIOR_LEFT_LOWER(
        14,
        "Left Lower Lung Field / Base"
    )
}