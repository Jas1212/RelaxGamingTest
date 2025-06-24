/**
* Relax Gaming - Java clean code test
*
* For the purposes of this test, the candidate can assume that the code compiles and that references
* to other classes do what you would expect them to.
*
* The objective is for the candidate to list down the things in plain text which can be improved in this class
*
* Good luck!
*
*/

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Here are my recommendations on what can be improved:
 * - Class names usually start with a capital, so Account instead of account.
 * - public String accountNumber should be private.
 * - if an account number is a plain number, an integer type should be used instead of String.
 * - for(i=0; i<dbTransactionList.size(); i++)it looks like a for-each over dbTransactionList would be more suited since i is not used.
 * - createTimestampAndExpirydate should be split into 2 methods (alternative a class with 2 properties could be returned).
 * - "trans.setTimestamp(createTimestampAndExpiryDate(trans)[0]);" and "trans.setExpiryDate(createTimestampAndExpiryDate(trans)[1]);" should use the new methods or improved createTimestampAndExpirydate described above.
 * - the 4 line in makeTransactionFromDbRow is commented out, impossible to tell if this is wrong but it looks like it.
 * - float currencyAmountInEuros = new Float(currencyAmountInPounds * 1.10); should use a constant instead (or property from other data), not 1.10 in code.
 * - not a big deal, bue fixDescription could return directlt with return "Transaction [" + desc + "]";.
 * - equals should take a Object as argument, and it needs to check that the object is the correct type (in this case Account). Also, it uses Account instead of account.
 * 
 */


public class account
{
    public String accountNumber;
    
    public account(String accountNumber){
        // Constructor
        this.accountNumber = accountNumber;
    }
    
    public String getAccountNumber(){
        return accountNumber; // return the account number
    }
    
    public ArrayList getTransactions() throws Exception{
        try{
            List dbTransactionList = Db.getTransactions(accountNumber.trim()); //Get the list of transactions
            ArrayList transactionList = new ArrayList();
            int i;
            for(i=0; i<dbTransactionList.size(); i++){
                DbRow dbRow = (DbRow) dbTransactionList.get(i);
                Transaction trans = makeTransactionFromDbRow(dbRow);
                trans.setTimestamp(createTimestampAndExpiryDate(trans)[0]);
                trans.setExpiryDate(createTimestampAndExpiryDate(trans)[1]);
                transactionList.add(trans);
            }
            return transactionList;
            
        } catch (SQLException ex){
            // There was a database error
            throw new Exception("Can't retrieve transactions from the database");
        }
    }
    
    public Transaction makeTransactionFromDbRow(DbRow row)
    {
        double currencyAmountInPounds = Double.parseDouble(row.getValueForField("amt"));
        float currencyAmountInEuros = new Float(currencyAmountInPounds * 1.10);
        String description = row.getValueForField("desc");
//        description = fixDescription(description);
        return new Transaction(description, currencyAmountInEuros); // return the new Transaction object
    }
    
    public String[] createTimestampAndExpirydate(Transaction trans) {
    	String[] return1 = new String[]{};
    	LocalDateTime now = LocalDateTime.now();
    	return1[0] = now.toString();
    	return1[1] = LocalDateTime.now().plusDays(60).toString();
    	
    	return return1;
    	
    }
    
    public String fixDescription(String desc) {
    	String newDesc = "Transaction [" + desc + "]";
    	return newDesc;
    }
    
    // Override the equals method   
    public boolean equals(Account o) {
        return o.getAccountNumber() == getAccountNumber(); // check account numbers are the same 
    }
}       

