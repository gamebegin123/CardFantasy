package cfvbaibai.cardfantasy.engine.feature;

import java.util.ArrayList;
import java.util.List;

import cfvbaibai.cardfantasy.GameUI;
import cfvbaibai.cardfantasy.data.Feature;
import cfvbaibai.cardfantasy.engine.CardInfo;
import cfvbaibai.cardfantasy.engine.FeatureEffect;
import cfvbaibai.cardfantasy.engine.FeatureEffectType;
import cfvbaibai.cardfantasy.engine.FeatureInfo;
import cfvbaibai.cardfantasy.engine.FeatureResolver;
import cfvbaibai.cardfantasy.engine.HeroDieSignal;
import cfvbaibai.cardfantasy.engine.OnDamagedResult;

public final class ChainAttackFeature {
    public static void apply(FeatureResolver resolver, FeatureInfo featureInfo, CardInfo attacker, CardInfo defender, Feature attackFeature)
            throws HeroDieSignal {
        if (attacker == null) {
            return;
        }
        if (attackFeature != null) {
            // Only normal attack can trigger ChainAttack.
            // Sweep & ChainAttack itself cannot trigger ChainAttack
            return;
        }
        Feature feature = featureInfo.getFeature();
        // Prevent stack overflow...
        if (!attacker.getEffectsCausedBy(featureInfo).isEmpty()) {
            return;
        }
        
        List<CardInfo> victims = new ArrayList<CardInfo>();
        for (CardInfo victim : defender.getOwner().getField().getAliveCards()) {
            if (victim == defender || !CardInfo.isSameType(victim, defender)) {
                continue;
            }
            victims.add(victim);
        }
        if (victims.isEmpty()) {
            return;
        }

        GameUI ui = resolver.getStage().getUI();

        ui.useSkill(attacker, victims, feature, true);

        int chainAT = feature.getImpact() * attacker.getLevel1AT() / 100;
        int adjAT = chainAT - attacker.getCurrentAT();
        FeatureEffect effect = new FeatureEffect(FeatureEffectType.ATTACK_CHANGE, featureInfo, adjAT, false);
        ui.adjustAT(attacker, attacker, adjAT, feature);
        attacker.addEffect(effect);
        for (CardInfo victim : victims) {
            OnDamagedResult damagedResult = resolver.attackCard(attacker, victim, featureInfo);
            if (damagedResult == null) {
                // 闪避导致连锁中断
                break;
            }
        }
        ui.loseAdjustATEffect(attacker, effect);
        attacker.removeEffect(effect);
    }
}
