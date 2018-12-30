package com.iota.iri;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.crypto.SpongeFactory;
import com.iota.iri.model.Hash;
import com.iota.iri.model.TransactionHash;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.Converter;
import com.iota.iri.zmq.MessageQ;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.iota.iri.controllers.TransactionViewModelTest.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionValidatorTest {

  private static final int MAINNET_MWM = 14;
  private static final int ZERO_MWM = 0;
  private static final TemporaryFolder dbFolder = new TemporaryFolder();
  private static final TemporaryFolder logFolder = new TemporaryFolder();
  private static Tangle tangle;
  private static TransactionValidator txValidator;

  @BeforeClass
  public static void setUp() throws Exception {
    dbFolder.create();
    logFolder.create();
    tangle = new Tangle();
    tangle.addPersistenceProvider(
        new RocksDBPersistenceProvider(
            dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),1000));
    tangle.init();
    TipsViewModel tipsViewModel = new TipsViewModel();
    MessageQ messageQ = Mockito.mock(MessageQ.class);
    TransactionRequester txRequester = new TransactionRequester(tangle, messageQ);
    txValidator = new TransactionValidator(tangle, tipsViewModel, txRequester);
    txValidator.setMwm(false, MAINNET_MWM);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    tangle.shutdown();
    dbFolder.delete();
    logFolder.delete();
  }

  @Test
  public void testMinMwm() throws InterruptedException {
    txValidator.init(false, 5);
    assertTrue(txValidator.getMinWeightMagnitude() == 13);
    txValidator.shutdown();
    txValidator.init(false, MAINNET_MWM);
  }

  @Test
  public void validateTrits() {
    byte[] trits = getRandomTransactionTrits();
    Converter.copyTrits(0, trits, 0, trits.length);
    txValidator.validateTrits(trits, MAINNET_MWM);
  }

  @Test(expected = RuntimeException.class)
  public void validateTritsWithInvalidMetadata() {
    byte[] trits = getRandomTransactionTrits();
    txValidator.validateTrits(trits, MAINNET_MWM);
  }

  @Test
  public void validateBytesWithNewCurl() throws Exception {
    byte[] trits = getRandomTransactionTrits();
    Converter.copyTrits(0, trits, 0, trits.length);
    byte[] bytes = Converter.allocateBytesForTrits(trits.length);
    Converter.bytes(trits, 0, bytes, 0, trits.length);
    txValidator.validateBytes(bytes, txValidator.getMinWeightMagnitude(), SpongeFactory.create(SpongeFactory.Mode.CURLP81));
  }

  @Test
  public void verifyTxIsSolid() throws Exception {
    TransactionViewModel tx = getTxWithBranchAndTrunk();
    assertTrue(txValidator.checkSolidity(tx.getHash()));
    assertTrue(txValidator.checkSolidity(tx.getHash()));
  }

  @Test
  public void verifyTxIsNotSolid() throws Exception {
    TransactionViewModel tx = getTxWithoutBranchAndTrunk();
    assertFalse(txValidator.checkSolidity(tx.getHash()));
    assertFalse(txValidator.checkSolidity(tx.getHash()));
  }

  @Test
  public void addSolidTransactionWithoutErrors() {
    byte[] trits = getRandomTransactionTrits();
    Converter.copyTrits(0, trits, 0, trits.length);
    txValidator.addSolidTransaction(TransactionHash.calculate(SpongeFactory.Mode.CURLP81, trits));
  }

  private TransactionViewModel getTxWithBranchAndTrunk() throws Exception {
    TransactionViewModel tx, trunkTx, branchTx;
    String trytes = "999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999CFDEZBLZQYA9999999999999999999999999999999999999999999ZZWQHWD99C99999999C99999999CKWWDBWSCLMQULCTAAJGXDEMFJXPMGMAQIHDGHRBGEMUYNNCOK9YPHKEEFLFCZUSPMCJHAKLCIBQSGWAS999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999";

    byte[] trits = Converter.allocateTritsForTrytes(trytes.length());
    Converter.trits(trytes, trits, 0);
    trunkTx = new TransactionViewModel(trits, TransactionHash.calculate(SpongeFactory.Mode.CURLP81, trits));
    branchTx = new TransactionViewModel(trits, TransactionHash.calculate(SpongeFactory.Mode.CURLP81, trits));

    byte[] childTx = getRandomTransactionTrits();
    System.arraycopy(trunkTx.getHash().trits(), 0, childTx, TransactionViewModel.TRUNK_TRANSACTION_TRINARY_OFFSET, TransactionViewModel.TRUNK_TRANSACTION_TRINARY_SIZE);
    System.arraycopy(branchTx.getHash().trits(), 0, childTx, TransactionViewModel.BRANCH_TRANSACTION_TRINARY_OFFSET, TransactionViewModel.BRANCH_TRANSACTION_TRINARY_SIZE);
    tx = new TransactionViewModel(childTx, TransactionHash.calculate(SpongeFactory.Mode.CURLP81, childTx));

    trunkTx.store(tangle);
    branchTx.store(tangle);
    tx.store(tangle);

    return tx;
  }

    @Test
    public void testTransactionPropagation() throws Exception {
        TransactionViewModel leftChildLeaf = TransactionTestUtils.createTransactionWithTrytes("CHILDTX");
        leftChildLeaf.updateSolid(true);
        leftChildLeaf.store(tangle);

        TransactionViewModel rightChildLeaf = TransactionTestUtils.createTransactionWithTrytes("CHILDTWOTX");
        rightChildLeaf.updateSolid(true);
        rightChildLeaf.store(tangle);

        TransactionViewModel parent = TransactionTestUtils.createTransactionWithTrunkAndBranch("PARENT",
                leftChildLeaf.getHash(), rightChildLeaf.getHash());
        parent.updateSolid(false);
        parent.store(tangle);

        TransactionViewModel parentSibling = TransactionTestUtils.createTransactionWithTrytes("PARENTLEAF");
        parentSibling.updateSolid(true);
        parentSibling.store(tangle);

        TransactionViewModel grandParent = TransactionTestUtils.createTransactionWithTrunkAndBranch("GRANDPARENT", parent.getHash(),
                        parentSibling.getHash());
        grandParent.updateSolid(false);
        grandParent.store(tangle);

        txValidator.addSolidTransaction(leftChildLeaf.getHash());
        while (!txValidator.isNewSolidTxSetsEmpty()) {
            txValidator.propagateSolidTransactions();
        }

        parent = TransactionViewModel.fromHash(tangle, parent.getHash());
        assertTrue("Parent tx was expected to be solid", parent.isSolid());
        grandParent = TransactionViewModel.fromHash(tangle, grandParent.getHash());
        assertTrue("Grandparent  was expected to be solid", grandParent.isSolid());
    }

  @Test
  public void testTransactionPropagationFailure() throws Exception {
    TransactionViewModel leftChildLeaf = new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash());
    leftChildLeaf.updateSolid(true);
    leftChildLeaf.store(tangle);

    TransactionViewModel rightChildLeaf = new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash());
    rightChildLeaf.updateSolid(true);
    rightChildLeaf.store(tangle);

    TransactionViewModel parent = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(leftChildLeaf.getHash(),
            rightChildLeaf.getHash()), getRandomTransactionHash());
    parent.updateSolid(false);
    parent.store(tangle);

    TransactionViewModel parentSibling = new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash());
    parentSibling.updateSolid(false);
    parentSibling.store(tangle);

    TransactionViewModel grandParent = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(parent.getHash(),
            parentSibling.getHash()), getRandomTransactionHash());
    grandParent.updateSolid(false);
    grandParent.store(tangle);

    txValidator.addSolidTransaction(leftChildLeaf.getHash());
    while (!txValidator.isNewSolidTxSetsEmpty()) {
      txValidator.propagateSolidTransactions();
    }

    parent = TransactionViewModel.fromHash(tangle, parent.getHash());
    assertTrue("Parent tx was expected to be solid", parent.isSolid());
    grandParent = TransactionViewModel.fromHash(tangle, grandParent.getHash());
    assertFalse("GrandParent tx was expected to be not solid", grandParent.isSolid());
  }

  @Test
  public void validateCorrectTimestamp() throws Exception {
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, 0);
    txValidator.runValidation(tx, ZERO_MWM);
  }

  @Test(expected = TransactionValidator.StaleTimestampException.class)
  public void failOnValidateInCorrectTimestamp() throws Exception {
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, -1);
    txValidator.runValidation(tx, ZERO_MWM);
  }

  @Test
  public void validateCurrentTimestamp() throws Exception {
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, System.currentTimeMillis() / 1000);
    txValidator.runValidation(tx, ZERO_MWM);
  }

  @Test(expected = TransactionValidator.StaleTimestampException.class)
  public void validate3HrFutureTimestamp() throws Exception {
    long threeHours = 60*60*3;
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, System.currentTimeMillis() / 1000 + threeHours);
    txValidator.runValidation(tx, ZERO_MWM);
  }

  @Test
  public void validateChangingEpochTimestamp() throws Exception {
    long threeHours = 60*60*3;
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, threeHours);
    TransactionValidator.setLatestEpochTimestamp(threeHours - 10);
    txValidator.runValidation(tx, ZERO_MWM);
    TransactionValidator.setLatestEpochTimestamp(0);
  }

  @Test(expected = TransactionValidator.StaleTimestampException.class)
  public void failOnValidateChangingEpochTimestamp() throws Exception {
    long threeHours = 60*60*3;
    TransactionViewModel tx = getValidTxWithoutBranchAndTrunk(0, threeHours);
    TransactionValidator.setLatestEpochTimestamp(threeHours + 10);
    txValidator.runValidation(tx, ZERO_MWM);
    TransactionValidator.setLatestEpochTimestamp(0);

  }

  @Test
  public void testClear() throws Exception {
    txValidator.addSolidTransaction(Hash.NULL_HASH);

    assertFalse(txValidator.isNewSolidTxSetsEmpty());

    txValidator.clearSolidTransactionsQueue();

    assertTrue(txValidator.isNewSolidTxSetsEmpty());
  }

  private TransactionViewModel getTxWithoutBranchAndTrunk() throws Exception {
    byte[] trits = getRandomTransactionTrits();
    TransactionViewModel tx = new TransactionViewModel(trits, TransactionHash.calculate(SpongeFactory.Mode.CURLP81, trits));
    tx.store(tangle);

    return tx;
  }

  private TransactionViewModel getValidTxWithoutBranchAndTrunk(long value, long timestamp) throws Exception {
    byte[] trits = getRandomTransactionTrits();
    //value
    byte[] valueT = new byte[TransactionViewModel.VALUE_TRINARY_SIZE];
    byte[] timestampT = new byte[TransactionViewModel.TIMESTAMP_TRINARY_SIZE];
    byte[] attachmentTimestampT = new byte[TransactionViewModel.ATTACHMENT_TIMESTAMP_TRINARY_SIZE];

    Converter.copyTrits(value, valueT, 0, valueT.length);
    Converter.copyTrits(timestamp, timestampT, 0, timestampT.length);
    Converter.copyTrits(timestamp * 1000, attachmentTimestampT, 0, attachmentTimestampT.length);

    System.arraycopy(valueT, 0, trits, TransactionViewModel.VALUE_TRINARY_OFFSET, valueT.length);
    System.arraycopy(timestampT, 0, trits, TransactionViewModel.TIMESTAMP_TRINARY_OFFSET, timestampT.length);
    System.arraycopy(attachmentTimestampT, 0, trits, TransactionViewModel.ATTACHMENT_TIMESTAMP_TRINARY_OFFSET, attachmentTimestampT.length);

    TransactionViewModel tx = new TransactionViewModel(trits, TransactionHash.calculate(SpongeFactory.Mode.CURLP81, trits));

    tx.store(tangle);

    return tx;
  }
}
