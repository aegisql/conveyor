package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;

import java.util.*;
import java.util.function.Consumer;

public class DuplicateValidator  <K,L,P> implements Consumer<Cart<K,?,L>> {

    private final String propertyName;
    private final Map<K,Map<L,Set<P>>> valueIds = new HashMap<>();

    public DuplicateValidator() {
        this("VALUE_ID");
    }

    public static <K,L> void wrap(Conveyor<K,L,?> conv) {
        DuplicateValidator<K,L,?> dv = new DuplicateValidator<>();
        conv.addCartBeforePlacementValidator(dv);
        conv.addBeforeKeyEvictionAction(dv.acknowledge());
    }

    public static <K,L> void wrap(Conveyor<K,L,?> conv, String propertyName) {
        DuplicateValidator<K,L,?> dv = new DuplicateValidator<>(propertyName);
        conv.addCartBeforePlacementValidator(dv);
        conv.addBeforeKeyEvictionAction(dv.acknowledge());
    }

    public DuplicateValidator(String propertyName) {
        Objects.requireNonNull(propertyName,"Duplicate Validator requires property name");
        this.propertyName = propertyName;
    }

    @Override
    public void accept(Cart<K, ?, L> cart) {
        P property = (P) cart.getAllProperties().get(propertyName);
        if(property == null) {
            return;
        }
        K key = cart.getKey();
        L label = cart.getLabel();
        Map<L, Set<P>> kMap = valueIds.computeIfAbsent(key, k -> new HashMap<>());
        Set<P> pSet = kMap.computeIfAbsent(label, l -> new HashSet<>());
        if(pSet.contains(property)) {
            throw new DuplicateValueException("Found duplicate record '"+key+"':'"+label+"' "+propertyName+"="+property + " value=" + cart.getValue());
        } else {
            pSet.add(property);
        }
    }

    public Consumer<AcknowledgeStatus<K>> acknowledge() {
        return acknowledgeStatus->{
            K key = acknowledgeStatus.getKey();
            valueIds.remove(key);
        };
    }
}
