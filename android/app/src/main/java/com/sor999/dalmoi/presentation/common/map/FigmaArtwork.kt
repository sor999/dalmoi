package com.sor999.dalmoi.presentation.common.map

// Asset geometry comes from the Figma MCP node bounds and SVG root dimensions.
data class SvgSlot(val file: String, val x: Float, val y: Float, val width: Float, val height: Float)
data class MapLabel(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val size: Float,
    val color: Long,
)
data class ArtworkSpec(val width: Float, val height: Float, val slots: List<SvgSlot>, val labels: List<MapLabel>)
enum class MapArtwork {
    Editor,
    EditorError,
    Course,
    ThumbLoop,
    ThumbReturn,
    ThumbIntervals,
    Waiting,
    Running,
    RunningError,
    Record,
    MvpEditor,
    MvpRunning,
    MvpRecord,
}

internal fun artworkSpec(artwork: MapArtwork): ArtworkSpec = when (artwork) {
    MapArtwork.Editor -> ArtworkSpec(
        344.0f,
        744.0f,
        slots = listOf(
            SvgSlot(
                "ad8f5a02-afec-4264-b978-a4cf10b227e4.svg",
                61.49950f,
                203.49950f,
                227.00100f,
                219.00100f,
            ),

            SvgSlot("6431c4d0-ed55-4ae4-a298-74ad5f3bac47.svg", 48.75000f, 404.75000f, 30.50000f, 30.50000f),
            SvgSlot("64659e26-2fa5-469d-8f4c-cf923cfe2340.svg", 94.00000f, 258.00000f, 8.00000f, 8.00000f),
            SvgSlot("64659e26-2fa5-469d-8f4c-cf923cfe2340.svg", 192.00000f, 202.00000f, 8.00000f, 8.00000f),
            SvgSlot("64659e26-2fa5-469d-8f4c-cf923cfe2340.svg", 282.00000f, 264.00000f, 8.00000f, 8.00000f),
            SvgSlot("c5ca83e7-4b79-44d6-88d9-e369c48a046a.svg", 237.00000f, 391.00000f, 34.00000f, 34.00000f),
            SvgSlot("2c474df6-7d03-4000-ade1-69cb2d80a6bf.svg", 242.00000f, 396.00000f, 24.00000f, 24.00000f),
        ),
        labels = listOf(
            MapLabel("S", 60.00000f, 413.00000f, 8.00000f, 15.00000f, 12.00000f, 0xFF141414),
            MapLabel("E", 250.00000f, 400.50000f, 8.00000f, 15.00000f, 12.00000f, 0xFFFFFFFF),
        ),
    )

    MapArtwork.EditorError -> ArtworkSpec(
        344.0f,
        744.0f,
        slots = listOf(
            SvgSlot(
                "79213c0e-159c-4420-b768-01368f5c092b.svg",
                61.49950f,
                203.49950f,
                227.00100f,
                219.00100f,
            ),

            SvgSlot("15fea3ac-300c-43c1-9548-cff991fdcee0.svg", 48.75000f, 404.75000f, 30.50000f, 30.50000f),
            SvgSlot("d55c67cf-83ff-48d9-b2b1-a5d02a0a7100.svg", 192.00000f, 202.00000f, 8.00000f, 8.00000f),
            SvgSlot("d55c67cf-83ff-48d9-b2b1-a5d02a0a7100.svg", 282.00000f, 264.00000f, 8.00000f, 8.00000f),
            SvgSlot("1f9ff64e-333f-46e1-ae8a-f8c4097536d3.svg", 237.00000f, 391.00000f, 34.00000f, 34.00000f),
            SvgSlot("3963b005-b30c-4e73-9748-3baa61dd3299.svg", 242.00000f, 396.00000f, 24.00000f, 24.00000f),
        ),
        labels = listOf(
            MapLabel("S", 60.00000f, 413.00000f, 8.00000f, 15.00000f, 12.00000f, 0xFF141414),
            MapLabel("E", 250.00000f, 400.50000f, 8.00000f, 15.00000f, 12.00000f, 0xFFFFFFFF),
        ),
    )

    MapArtwork.Course -> ArtworkSpec(
        312.0f,
        300.0f,
        slots = listOf(
            SvgSlot("7ce8a734-5ddc-4484-9032-a1033a62d779.svg", 45.50000f, 59.50000f, 225.00000f, 179.00000f),
            SvgSlot("972fe2fe-211e-48a1-9393-a543dd337271.svg", 35.75000f, 197.75000f, 24.50000f, 24.50000f),
        ),
        labels = listOf(
            MapLabel("S/E", 39.00000f, 203.50000f, 19.00000f, 13.00000f, 11.00000f, 0xFF141414),
        ),
    )

    MapArtwork.ThumbLoop -> ArtworkSpec(
        56.0f,
        56.0f,
        slots = listOf(
            SvgSlot("e2626b99-0714-4c76-bb3c-f3135358a18f.svg", 0.00000f, 0.00000f, 56.00000f, 56.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.ThumbReturn -> ArtworkSpec(
        56.0f,
        56.0f,
        slots = listOf(
            SvgSlot("99c66a64-2283-4b84-80ba-07d97689cc4e.svg", 0.00000f, 0.00000f, 56.00000f, 56.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.ThumbIntervals -> ArtworkSpec(
        56.0f,
        56.0f,
        slots = listOf(
            SvgSlot("fb2ebe67-b7cc-49c0-9338-fa885ad84a19.svg", 0.00000f, 0.00000f, 56.00000f, 56.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.Waiting -> ArtworkSpec(
        304.0f,
        150.0f,
        slots = listOf(
            SvgSlot("5b926337-2a58-4bbb-9213-1abbd76351a1.svg", 0.00000f, 0.00000f, 304.00000f, 150.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.Running -> ArtworkSpec(
        344.0f,
        480.0f,
        slots = listOf(
            SvgSlot(
                "6e070fac-02ae-4896-a239-62f73fde86d8.svg",
                57.50000f,
                117.50000f,
                245.00000f,
                265.00000f,
            ),

            SvgSlot(
                "bc05753c-2d77-448d-b10f-c978fc44ed56.svg",
                57.50000f,
                135.50000f,
                103.00000f,
                247.00000f,
            ),

            SvgSlot("fcbaf316-5840-416a-916c-0093f8f738d6.svg", 51.75000f, 371.75000f, 16.50000f, 16.50000f),
            SvgSlot("d4df70b6-f78d-440d-985b-817ce1a7d544.svg", 136.00000f, 116.00000f, 44.00000f, 44.00000f),
            SvgSlot("92facc86-5bef-4b25-ba5b-eb1b56316999.svg", 143.50000f, 123.50000f, 29.00000f, 29.00000f),
            SvgSlot("2ed8b193-29de-40c9-9bf1-02b8eab8156f.svg", 217.50000f, 119.50000f, 29.00000f, 29.00000f),
            SvgSlot("5db0e80f-db82-46de-a485-f31e5ce2a13f.svg", 65.75000f, 235.75000f, 28.50000f, 28.50000f),
            SvgSlot("b36df33d-2ebe-44f7-833b-4715de82163c.svg", 98.00000f, 240.00000f, 62.00000f, 20.00000f),
        ),
        labels = listOf(
            MapLabel("민", 153.00000f, 131.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFF121211),
            MapLabel("서", 227.00000f, 127.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFF121211),
            MapLabel("지", 75.00000f, 243.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFFe8a13a),
            MapLabel("42초 전", 109.50000f, 243.00000f, 38.00000f, 13.00000f, 11.00000f, 0xFF121211),
        ),
    )

    MapArtwork.RunningError -> ArtworkSpec(
        344.0f,
        480.0f,
        slots = listOf(
            SvgSlot(
                "0fc89285-a862-445c-bec9-7fe834b6dd43.svg",
                57.50000f,
                117.50000f,
                245.00000f,
                265.00000f,
            ),

            SvgSlot(
                "640fb3b5-084a-462e-8272-e17074bdc818.svg",
                57.50000f,
                135.50000f,
                103.00000f,
                247.00000f,
            ),

            SvgSlot("d07e33d7-5592-489a-b5ec-f7594493776f.svg", 51.75000f, 371.75000f, 16.50000f, 16.50000f),
            SvgSlot("a7783a13-8d2c-4fd5-94eb-a21fb26d4ae5.svg", 136.00000f, 116.00000f, 44.00000f, 44.00000f),
            SvgSlot("27448fc2-d0a4-4b84-808c-758f87aa7f73.svg", 143.50000f, 123.50000f, 29.00000f, 29.00000f),
            SvgSlot("ebc9f453-969f-4e25-9665-55431f1b7ece.svg", 217.50000f, 119.50000f, 29.00000f, 29.00000f),
            SvgSlot("a447d26d-e34b-46e6-8516-f200bbbfe3d3.svg", 65.75000f, 235.75000f, 28.50000f, 28.50000f),
            SvgSlot("eccedbe1-46a9-4c0a-976e-c53ffd6047cc.svg", 98.00000f, 240.00000f, 62.00000f, 20.00000f),
        ),
        labels = listOf(
            MapLabel("민", 153.00000f, 131.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFF121211),
            MapLabel("서", 227.00000f, 127.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFF121211),
            MapLabel("지", 75.00000f, 243.50000f, 11.00000f, 13.00000f, 11.00000f, 0xFFe8a13a),
            MapLabel("42초 전", 109.50000f, 243.00000f, 38.00000f, 13.00000f, 11.00000f, 0xFF121211),
        ),
    )

    MapArtwork.Record -> ArtworkSpec(
        304.0f,
        220.0f,
        slots = listOf(
            SvgSlot("4752f7cf-665f-4ea5-b8ff-eb6cb0c7089f.svg", 0.00000f, 0.00000f, 304.00000f, 220.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.MvpEditor -> ArtworkSpec(
        360.0f,
        800.0f,
        slots = listOf(
            SvgSlot("c73b0327-cd20-4f5b-9771-289e09a58618.svg", 0.00000f, 0.00000f, 360.00000f, 800.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.MvpRunning -> ArtworkSpec(
        360.0f,
        800.0f,
        slots = listOf(
            SvgSlot("1401f772-66c4-4df3-ba65-231c8be132b9.svg", 0.00000f, 0.00000f, 360.00000f, 800.00000f),
        ),
        labels = listOf(),
    )

    MapArtwork.MvpRecord -> ArtworkSpec(
        320.0f,
        260.0f,
        slots = listOf(
            SvgSlot("2f6543ca-23e9-4d32-beab-3fb2a790b782.svg", 0.00000f, 0.00000f, 320.00000f, 260.00000f),
        ),
        labels = listOf(),
    )
}
