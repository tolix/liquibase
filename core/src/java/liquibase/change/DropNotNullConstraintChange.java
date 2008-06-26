package liquibase.change;

import liquibase.database.Database;
import liquibase.database.SQLiteDatabase;
import liquibase.database.SQLiteDatabase.AlterTableVisitor;
import liquibase.database.sql.SetNullableStatement;
import liquibase.database.sql.SqlStatement;
import liquibase.database.structure.Column;
import liquibase.database.structure.DatabaseObject;
import liquibase.database.structure.Index;
import liquibase.database.structure.Table;
import liquibase.exception.JDBCException;
import liquibase.exception.UnsupportedChangeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Drops a not-null constraint from an existing column.
 */
public class DropNotNullConstraintChange extends AbstractChange {

    private String schemaName;
    private String tableName;
    private String columnName;
    private String columnDataType;


    public DropNotNullConstraintChange() {
        super("dropNotNullConstraint", "Drop Not-Null Constraint");
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnDataType() {
        return columnDataType;
    }

    public void setColumnDataType(String columnDataType) {
        this.columnDataType = columnDataType;
    }

    public SqlStatement[] generateStatements(Database database) throws UnsupportedChangeException {
    	List<SqlStatement> statements = new ArrayList<SqlStatement>();
    	
    	if (database instanceof SQLiteDatabase) {
    		// SQLite does not support this ALTER TABLE operation until now.
			// For more information see: http://www.sqlite.org/omitted.html.
			// This is a small work around...
    		
			// define alter table logic
    		AlterTableVisitor rename_alter_visitor = new AlterTableVisitor() {
    			public ColumnConfig[] getColumnsToAdd() {
    				return new ColumnConfig[0];
    			}
    			public boolean copyThisColumn(ColumnConfig column) {
    				return true;
    			}
    			public boolean createThisColumn(ColumnConfig column) {
    				if (column.getName().equals(getColumnName())) {
    					column.getConstraints().setNullable(true);
    				}
    				return true;
    			}
    			public boolean createThisIndex(Index index) {
    				return true;
    			}
    		};
        		
        	try {
        		// alter table
				statements.addAll(SQLiteDatabase.getAlterTableStatements(
						rename_alter_visitor,
						database,getSchemaName(),getTableName()));
    		} catch (JDBCException e) {
				e.printStackTrace();
			}
    	} else {
    		
    		// ...if it is not a SQLite database 
    		statements.add(new SetNullableStatement(getSchemaName() == null?database.getDefaultSchemaName():getSchemaName(), getTableName(), getColumnName(), getColumnDataType(), true));
    		
    	}
    	return statements.toArray(new SqlStatement[statements.size()]);
    }

    protected Change[] createInverses() {
        AddNotNullConstraintChange inverse = new AddNotNullConstraintChange();
        inverse.setColumnName(getColumnName());
        inverse.setSchemaName(getSchemaName());
        inverse.setTableName(getTableName());
        inverse.setColumnDataType(getColumnDataType());

        return new Change[]{
                inverse
        };
    }

    public String getConfirmationMessage() {
        return "Null constraint dropped from " + getTableName() + "." + getColumnName();
    }

    public Element createNode(Document currentChangeLogFileDOM) {
        Element element = currentChangeLogFileDOM.createElement("dropNotNullConstraint");
        if (getSchemaName() != null) {
            element.setAttribute("schemaName", getSchemaName());
        }

        element.setAttribute("tableName", getTableName());
        element.setAttribute("columnName", getColumnName());
        return element;
    }

    public Set<DatabaseObject> getAffectedDatabaseObjects() {

        Table table = new Table(getTableName());

        Column column = new Column();
        column.setTable(table);
        column.setName(columnName);


        return new HashSet<DatabaseObject>(Arrays.asList(table, column));
    }
}
