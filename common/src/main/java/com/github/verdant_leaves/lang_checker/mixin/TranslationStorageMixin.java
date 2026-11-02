package com.github.verdant_leaves.lang_checker.mixin;

import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableContents.class)
public class TranslationStorageMixin {

    // 拦截构造函数，捕获硬编码 fallback
    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("TAIL"))
    private void onInit(String key, String fallback, Object[] args, CallbackInfo ci) {
        TranslationCollector.recordRawComponent(key, fallback);
    }
}