package jp.s12kuma01.actiniumextra.renderer.actinium;

import jp.s12kuma01.actiniumextra.client.ActiniumExtraClientMod;
import jp.s12kuma01.actiniumextra.client.gui.ActiniumExtraGameOptions;
import dhj.embeddedt.embeddium.api.options.structure.OptionStorage;

/**
 * Bridges Actinium Extra's settings into Actinium's options framework.
 * <p>
 * Actinium reads and persists every row it paints through this interface, so its settings screen can
 * show and store Actinium Extra's own configuration; the backing
 * {@link ActiniumExtraGameOptions} instance is supplied by {@link ActiniumExtraClientMod}.
 */
final class ActiniumOptionStorage implements OptionStorage<ActiniumExtraGameOptions> {

    @Override
    public ActiniumExtraGameOptions getData() {
        return ActiniumExtraClientMod.options();
    }

    @Override
    public void save() {
        ActiniumExtraClientMod.options().writeChanges();
    }
}
