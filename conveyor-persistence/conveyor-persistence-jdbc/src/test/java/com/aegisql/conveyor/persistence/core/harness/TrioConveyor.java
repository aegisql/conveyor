package com.aegisql.conveyor.persistence.core.harness;

import java.util.concurrent.PriorityBlockingQueue;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;



public class TrioConveyor extends AssemblingConveyor<Integer, SmartLabel<TrioBuilder>, Trio> {
	
	public final ResultMap<Integer, Trio> results = new ResultMap<>();
	public final ResultCounter<Integer, Trio> counter = new ResultCounter<>();

	public TrioConveyor() {
		super();
		this.setName("TrioConveyor");
		this.setBuilderSupplier(TrioBuilder::new);
		this.resultConsumer(LogResult.debug(this)).andThen(counter).andThen(results).set();
		this.scrapConsumer(LogScrap.error(this)).set();
		this.setReadinessEvaluator(Conveyor.getTesterFor(this).accepted(TrioPart.TEXT1,TrioPart.TEXT2,TrioPart.NUMBER));
	}

	public TrioConveyor(boolean parallel) {
		super(PriorityBlockingQueue::new);
		this.setName("TrioConveyor");
		this.setBuilderSupplier(TrioBuilder::new);
		this.resultConsumer(LogResult.debug(this)).andThen(counter).andThen(results).set();
		this.scrapConsumer(LogScrap.error(this)).set();
		this.setReadinessEvaluator(Conveyor.getTesterFor(this).accepted(TrioPart.TEXT1,TrioPart.TEXT2,TrioPart.NUMBER));
	}

	
	@Override
	public String toString() {
		return this.getName()+" [count= "+counter.get()+" results=" + results + "]";
	}
	
	
}
