package dev.mxxfoxx.actiniumextra.renderer.celeritas;

import dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod;
import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions;
import org.taumc.celeritas.api.options.structure.OptionStorage;

/**
 * Bridges Actinium Extra's settings into Celeritas' options framework.
 * <p>
 * Celeritas reads and persists every row it paints through this interface, so the GUI can show and
 * store Actinium Extra's own configuration; the backing {@link ActiniumExtraGameOptions} instance
 * is supplied by {@link ActiniumExtraClientMod}.
 */
final class CeleritasOptionStorage implements OptionStorage<ActiniumExtraGameOptions> {

    @Override
    public ActiniumExtraGameOptions getData() {
        return ActiniumExtraClientMod.options();
    }

    @Override
    public void save() {
        ActiniumExtraClientMod.options().writeChanges();
    }
}
