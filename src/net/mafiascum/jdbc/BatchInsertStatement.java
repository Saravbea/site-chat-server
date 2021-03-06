package net.mafiascum.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;

import net.mafiascum.enumerator.VEnum;
import net.mafiascum.util.QueryUtil;
import net.mafiascum.util.SQLUtil;

public class BatchInsertStatement {

  private static final int DEFAULT_BATCH_SIZE = 100;

  private Connection connection;
  private Statement statement;

  private String header;
  private StringBuffer sql;

  private int batchSize;
  private int numRowsLoaded;

  private boolean loadingRow;
  private boolean firstField;

  private int numFieldsPerRow;
  private int numFieldsLoaded;

  private int rows_inserted;
  
  public BatchInsertStatement (Connection connection, String table) throws SQLException {
    this(connection, table, DEFAULT_BATCH_SIZE, false);
  }

  public BatchInsertStatement (Connection connection, String table, int batchSize) throws SQLException {
    this(connection, table, batchSize, false);
  }

  public BatchInsertStatement (Connection connection, String table, int batchSize, boolean insertIgnore) throws SQLException {
    this.connection = connection;
    this.batchSize = batchSize;

    sql = new StringBuffer(batchSize * 10);
    if (insertIgnore)
      sql.append("INSERT IGNORE INTO ");
    else
      sql.append("INSERT INTO ");
    sql.append(table);
    sql.append('(');

    loadingRow = false;
    firstField = true;
    numFieldsLoaded = 0;
    rows_inserted = 0;
  }
  
  public BatchInsertStatement (Statement statement, String table) throws SQLException {
    this(statement, table, DEFAULT_BATCH_SIZE, false);
  }

  public BatchInsertStatement (Statement statement, String table, int batchSize) throws SQLException {
    this(statement, table, batchSize, false);
  }

  public BatchInsertStatement (Statement statement, String table, int batchSize, boolean insertIgnore) throws SQLException {
    this.statement = statement;
    this.batchSize = batchSize;

    sql = new StringBuffer(batchSize * 10);
    if (insertIgnore)
      sql.append("INSERT IGNORE INTO ");
    else
      sql.append("INSERT INTO ");
    sql.append(table);
    sql.append('(');

    loadingRow = false;
    firstField = true;
    numFieldsLoaded = 0;
    rows_inserted = 0;
  }

  public void addField (String fieldName) throws SQLException {
    if (firstField)
      firstField = false;
    else
      sql.append(',');

    sql.append(fieldName);
    numFieldsLoaded++;
  }

  public void start () throws SQLException {
    sql.append(")VALUES");
    header = sql.toString();

    if(statement == null)
      statement = connection.createStatement();

    numRowsLoaded = 0;
    numFieldsPerRow = numFieldsLoaded;

    sql.setLength(0);
    sql.append(header);
  }

  public Integer finish () throws SQLException {
    Integer last_inserted_id = flush();
    sql = null;

    if(connection != null) {
      statement.close();
      statement = null;
    }
    
    return last_inserted_id;
  }

  public void beginEntry () throws SQLException {
    if (loadingRow)
      throw new SQLException("Already loading row");

    if (numRowsLoaded > 0)
      sql.append(',');

    sql.append('(');

    loadingRow = true;
    firstField = true;
    numFieldsLoaded = 0;
  }

  public Integer endEntry () throws SQLException {
    
    Integer last_inserted_id = null;
    
    if (!loadingRow)
      throw new SQLException("Not loading row");

    if (numFieldsLoaded != numFieldsPerRow)
      throw new SQLException("Not enough fields loaded for this entry");

    sql.append(')');

    loadingRow = false;

    if (++numRowsLoaded == batchSize)
      last_inserted_id = flush();
    
    return last_inserted_id;
  }
  
  public Integer flush () throws SQLException {

    Integer last_inserted_id = null;
    
    if (loadingRow)
      throw new SQLException("Currently loading row");

    if (numRowsLoaded != 0) {
      rows_inserted = statement.executeUpdate(sql.toString());
      
      last_inserted_id = QueryUtil.get().getLastInsertedID(statement);
      
      numRowsLoaded = 0;

      sql.setLength(0);
      sql.append(header);
    }
    
    return last_inserted_id;
  }
  
  public int getRowsInserted () {
    return this.rows_inserted;
  }

  private void addFieldValue (String value) throws SQLException {
    if (!loadingRow)
      throw new SQLException("Not loading row");

    if (firstField) {
      firstField = false;
    }
    else {
      sql.append(',');
    }

    sql.append(value);
    numFieldsLoaded++;
  }

  public void putShort (short value) throws SQLException {
    addFieldValue(String.valueOf(value));
  }
  
  public void putInt (int value) throws SQLException {
    addFieldValue(String.valueOf(value));
  }

  public void putLong (long value) throws SQLException {
    addFieldValue(String.valueOf(value));
  }

  public void putShort (Short value) throws SQLException {
    addFieldValue((value != null) ? String.valueOf(value) : "NULL");
  }

  public void putInteger (Integer value) throws SQLException {
    addFieldValue((value != null) ? String.valueOf(value) : "NULL");
  }

  public void putLong (Long value) throws SQLException {
    addFieldValue((value != null) ? String.valueOf(value) : "NULL");
  }

  public void putDouble (double value) throws SQLException {
    addFieldValue(String.valueOf(value));
  }

  public void putDouble (Double value) throws SQLException {
    addFieldValue((value != null) ? String.valueOf(value) : "NULL");
  }

  public void putBigDecimal(BigDecimal value) throws SQLException {
    addFieldValue((value != null) ? value.toString() : "NULL");
  }

  public void putBool (boolean value) throws SQLException {
    addFieldValue(value ? "1" : "0");
  }

  public void putBoolean (Boolean value) throws SQLException {
    addFieldValue((value != null) ? (value.booleanValue() ? "1" : "0") : "NULL");
  }

  public void putVEnum (VEnum venum) throws SQLException {
    addFieldValue((venum != null) ? String.valueOf(venum.value()) : "NULL");
  }

  public void putString (String value) throws SQLException {
    addFieldValue((value != null) ? SQLUtil.get().escapeQuoteString(value) : "NULL");
  }

  public void putDate (Date value) throws SQLException {
    addFieldValue((value != null) ? SQLUtil.get().encodeQuoteTimestamp(value) : "NULL");
  }
  
  public void putDate (java.util.Date value) throws SQLException {
    addFieldValue((value != null) ? SQLUtil.get().encodeQuoteDate(value) : "NULL");
  }

  public void putMoney (BigDecimal value) throws SQLException {
    putFixedPoint(value, 2);
  }

  public void putFixedPoint (BigDecimal value, int scale) throws SQLException {
    addFieldValue((value != null) ? String.valueOf(value.movePointRight(scale).intValue()) : "NULL");
  }
}