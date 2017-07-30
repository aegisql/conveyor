package org.conveyor.persistence.core.harness;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.scrap.LogScrap;

public class TrioConveyor extends AssemblingConveyor<Integer, SmartLabel<TrioBuilder>, Trio> {
	public TrioConveyor() {
		this.setName("TrioConveyor");
		this.setBuilderSupplier(TrioBuilder::new);
		this.resultConsumer(LogResult.debug(this)).set();
		this.scrapConsumer(LogScrap.error(this)).set();
		this.setReadinessEvaluator(Conveyor.getTesterFor(this).accepted(TrioPart.TEXT1,TrioPart.TEXT2,TrioPart.NUMBER));
	}
}
