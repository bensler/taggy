package com.bensler.taggy.persist;

import org.hibernate.Transaction;

import com.bensler.decaf.util.io.AutoCloseableAdapter;

public class AutoCloseableTxn extends AutoCloseableAdapter<Transaction> {

  public AutoCloseableTxn(Transaction txn) {
    super(txn, aTxn -> aTxn.commit());
  }

}