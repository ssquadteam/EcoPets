# ModelEngine Integration for EcoPets

This document describes the implementation of ModelEngine integration in the EcoPets plugin, specifically for the R4.0.8 version of ModelEngine.

## Overview

The `ModelEnginePetEntity` class handles the creation and animation of custom pet models using ModelEngine. The implementation uses reflection extensively to maintain compatibility with different versions of ModelEngine while adapting to the specific API of ModelEngine R4.0.8.

## Key Features

1. **Flexible API Access**: The code attempts to access the ModelEngine API through various methods and fields to ensure compatibility with different versions.

2. **Multiple Animation Approaches**: Various methods to play animations are attempted in order:
   - Standard R4.0.8 playAnimation method with signature:
     ```java
     playAnimation(String animation, double lerpIn, double lerpOut, double speed, boolean force)
     ```
   - Enum-based state methods
   - Single-parameter playAnimation
   - Other animation-related methods

3. **Parameter Adaptation**: Automatic adaptation to different parameter types for animation methods.

4. **Fallback Mechanisms**: Falls back to ModelEngineBridge if direct API access fails.

5. **Extensive Debugging**: When debug logging is enabled, provides detailed information about method discovery and animation attempts.

## API Methods

The implementation primarily targets these ModelEngine R4.0.8 API methods:

1. **Static Methods**:
   - `ModelEngineAPI.getAPI()` - Gets the API instance
   - `ModelEngineAPI.createActiveModel(String modelId)` - Creates a model
   - `ModelEngineAPI.createModeledEntity(Entity entity)` - Creates an entity for the model

2. **Animation Methods**:
   - `AnimationHandler.playAnimation(String animation, double lerpIn, double lerpOut, double speed, boolean force)`
   - Various fallback methods for animation control

## Troubleshooting

If animations are not playing correctly:

1. Enable debug logging in the EcoPets config
2. Check the server logs for detailed information about API access and animation method calls
3. Verify that the animation name is correct and matches what's defined in the model
4. Ensure the model is properly loaded by ModelEngine 