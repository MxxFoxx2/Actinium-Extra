package jp.s12kuma01.actiniumextra.client.gui.spec;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A renderer-independent description of when an option control is enabled.
 * <p>
 * Sub-options have to grey out while their parent switch is off. The pre-existing page code did
 * that by capturing the already-built parent option object; a specification cannot hold such a
 * backend object, so the dependency is expressed by the parent's language key
 * ({@link ByKey}) and resolved by the renderer adapter against the option it just built.
 */
public sealed interface GateSpec {
    /** Gate that is always satisfied; the option is never greyed out. */
    GateSpec ALWAYS = new Always();

    /**
     * @return the always-enabled gate
     */
    static GateSpec always() {
        return ALWAYS;
    }

    /**
     * Follows the live (not yet necessarily applied) value of another option in the same
     * specification.
     *
     * @param optionKey language key of the parent option, which must be declared before this one
     * @return the parent-value gate
     */
    static GateSpec key(String optionKey) {
        return new ByKey(optionKey);
    }

    /**
     * Reads an environment condition the addon owns, such as whether an asset is present.
     *
     * @param supplier the condition
     * @return the supplier gate
     */
    static GateSpec live(BooleanSupplier supplier) {
        return new Live(supplier);
    }

    /**
     * Requires every listed gate to hold.
     *
     * @param gates the gates to combine
     * @return the conjunction gate
     */
    static GateSpec all(GateSpec... gates) {
        return new All(List.of(gates));
    }

    /** Always-true gate. */
    record Always() implements GateSpec {
    }

    /**
     * Gate mirroring another option's current GUI value.
     *
     * @param optionKey language key of the parent option
     */
    record ByKey(String optionKey) implements GateSpec {
    }

    /**
     * Gate backed by an addon-side condition.
     *
     * @param supplier the condition
     */
    record Live(BooleanSupplier supplier) implements GateSpec {
    }

    /**
     * Conjunction of nested gates.
     *
     * @param gates the gates to combine
     */
    record All(List<GateSpec> gates) implements GateSpec {
    }
}
