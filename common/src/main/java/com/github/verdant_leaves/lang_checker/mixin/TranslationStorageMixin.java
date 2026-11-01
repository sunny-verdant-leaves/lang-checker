package com.github.verdant_leaves.lang_checker.mixin;

import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class TranslationStorageMixin {

    // 正确的双参数方法描述符
    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"))
    private void onGetOrDefault(String key, String defaultValue, CallbackInfoReturnable<String> cir) {
        String result = cir.getReturnValue();
        
        if (result != null && !result.equals(key) && !TranslationCollector.availableKeys.contains(key)) {
            TranslationCollector.record(key, result);
        }
    }
}