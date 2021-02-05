package com.aegisql.conveyor.utils.results_depo;

import com.aegisql.conveyor.SmartLabel;

import java.util.function.BiConsumer;

public enum DepotLabel implements SmartLabel<ResultsDepot> {
    RESULT {
        @Override
        public BiConsumer<ResultsDepot, Object> get() {
            return null;
        }
    }
    ,SCRAP {
        @Override
        public BiConsumer<ResultsDepot, Object> get() {
            return null;
        }
    }
    ,SUBSCRIBE {
        @Override
        public BiConsumer<ResultsDepot, Object> get() {
            return null;
        }
    }
    ;
}
