package com.nyrds.android.util;

import com.nyrds.pixeldungeon.ml.EventCollector;
import com.watabou.pixeldungeon.utils.GLog;

/**
 * Created by DeadDie on 18.03.2016
 */
public class TrackedRuntimeException extends RuntimeException {

	public TrackedRuntimeException( Exception e) {
		super(e);
		EventCollector.logException(e,"");
	}

	public TrackedRuntimeException( String s) {
		super(s);
		EventCollector.logException(this,s);
	}

    public TrackedRuntimeException( String s,Exception e) {
        super(s,e);
        EventCollector.logException(this,s);
    }
}
