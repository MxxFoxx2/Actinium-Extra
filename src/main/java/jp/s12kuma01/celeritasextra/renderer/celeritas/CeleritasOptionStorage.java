package jp.s12kuma01.celeritasextra.renderer.celeritas;

import jp.s12kuma01.celeritasextra.client.CeleritasExtraClientMod;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions;
import org.taumc.celeritas.api.options.structure.OptionStorage;

/**
 * Bridges Celeritas Extra's settings into Celeritas' options framework.
 * <p>
 * Celeritas reads and persists every row it paints through this interface, so the GUI can show and
 * store Celeritas Extra's own configuration; the backing {@link CeleritasExtraGameOptions} instance
 * is supplied by {@link CeleritasExtraClientMod}.
 */
final class CeleritasOptionStorage implements OptionStorage<CeleritasExtraGameOptions> {

    @Override
    public CeleritasExtraGameOptions getData() {
        return CeleritasExtraClientMod.options();
    }

    @Override
    public void save() {
        CeleritasExtraClientMod.options().writeChanges();
    }
}
