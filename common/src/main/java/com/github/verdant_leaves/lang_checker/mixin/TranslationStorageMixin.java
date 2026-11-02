package com.github.verdant_leaves.lang_checker.mixin;

import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableContents.class)
public class TranslationStorageMixin {

    // 只拦截唯一存在的构造函数，同时捕获 key 和 fallback
    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("TAIL"))
    private void onInit(String key, String fallback, Object[] args, CallbackInfo ci) {
        // 将原始的 key 和 fallback 交给收集器处理
        TranslationCollector.recordRawComponent(key, fallback);
    }
}