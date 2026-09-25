package jp.s12kuma01.celeritasextra.renderer.actinium;

import jp.s12kuma01.celeritasextra.client.CeleritasExtraClientMod;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions;
import dhj.embeddedt.embeddium.api.options.structure.OptionStorage;

/**
 * Bridges Celeritas Extra's settings into Actinium's options framework.
 * <p>
 * Actinium reads and persists every row it paints through this interface, so its settings screen can
 * show and store Celeritas Extra's own configuration; the backing
 * {@link CeleritasExtraGameOptions} instance is supplied by {@link CeleritasExtraClientMod}.
 */
final class ActiniumOptionStorage implements OptionStorage<CeleritasExtraGameOptions> {

    @Override
    public CeleritasExtraGameOptions getData() {
        return CeleritasExtraClientMod.options();
    }

    @Override
    public void save() {
        CeleritasExtraClientMod.options().writeChanges();
    }
}
