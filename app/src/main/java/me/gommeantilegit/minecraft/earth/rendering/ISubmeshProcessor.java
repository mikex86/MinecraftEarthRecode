package me.gommeantilegit.minecraft.earth.rendering;

import com.google.ar.sceneform.rendering.Material;

import org.jetbrains.annotations.NotNull;

public interface ISubmeshProcessor {

    void onSubmeshCreation(int x, int y, int z, int sideIndex, int materialIndex, @NotNull Material material);

}
