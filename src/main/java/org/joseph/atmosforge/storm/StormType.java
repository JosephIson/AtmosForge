package org.joseph.atmosforge.storm;

public enum StormType {

    NONE,

    // Purely precip-driven, weak lift/instability
    SHOWERS,

    // Convective storms driven by instability + lift
    THUNDERSTORMS,

    // Strong shear + strong instability + strong updraft
    SUPERCELL,

    // Organized convective cluster (broad precip + sustained updraft)
    MCS,

    // Synoptic low / frontal cyclone precipitation shield
    EXTRATROPICAL_CYCLONE
}
