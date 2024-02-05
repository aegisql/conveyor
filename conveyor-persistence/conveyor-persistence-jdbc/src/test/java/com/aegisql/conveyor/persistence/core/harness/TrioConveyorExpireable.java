package com.aegisql.conveyor.persistence.core.harness;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.result.ResultMap;
import com.aegisql.conveyor.consumers.scrap.LogScrap;

import java.util.concurrent.TimeUnit;



public class TrioConveyorExpireable extends AssemblingConveyor<Integer, SmartLabel<TrioBuilderExpireable>, Trio> {
	
	public final ResultMap<Integer, Trio> results = new ResultMap<>();
	public final ResultCounter<Integer, Trio> counter = new ResultCounter<>();
	
	public TrioConveyorExpireable() {
		this.setName("TrioConveyorExpireable");
		this.setBuilderSupplier(TrioBuilderExpireable::new);
		this.setIdleHeartBeat(100, TimeUnit.MILLISECONDS);
		this.resultConsumer(LogResult.debug(this)).andThen(counter).andThen(results).set();
		this.scrapConsumer(LogScrap.error(this)).andThen(bin->{if(bin.error != null)bin.error.printStackTrace();}).set();
		this.setReadinessEvaluator(Conveyor.getTesterFor(this).accepted(TrioPartExpireable.TEXT1,TrioPartExpireable.TEXT2,TrioPartExpireable.NUMBER));
	}

	@Override
	public String toString() {
		return this.getName()+" [count= "+counter.get()+" results=" + results + "]";
	}
	
}
