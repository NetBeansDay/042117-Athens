package ds.todo.persistence;

import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ds.todo.Task;

public final class TaskManager {

    private final Parameters params;
    private Connection con;
    private Statement stmt;
    private ResultSet rs;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    private TaskManager(Parameters params) {
        this.params = params;
        try {
            connect();
        } catch (DatabaseException ex) {
            Logger.getLogger(TaskManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final static class SingletonHolder {

        private final static TaskManager INSTANCE = new TaskManager(new Parameters());
    }

    public static TaskManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    public Parameters getParams() {
        return params;
    }

    public void reconnect(String database) throws DatabaseException {
        disconnect();
        params.setDatabase(database);
        connect();
    }

    private void connect() throws DatabaseException {
        try {
            Class.forName(params.getJdbcDriver());
            con = DriverManager.getConnection(params.getJdbcUrl(), "sa", "");
            if (!checkTables()) {
                createTables();
            }
        } catch (DatabaseException e) {
            throw new DatabaseException("Cannot initialize the database tables", e.getCause());
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("Cannot load the database driver", e);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot open the database", e);
        }
    }

    private boolean checkTables() {
        try {
            String sql = "SELECT COUNT(*) FROM PUBLIC.todo";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            cleanUp();
        }
    }

    private void createTables() throws DatabaseException {
        update("CREATE TABLE PUBLIC.todo ("
                + "id IDENTITY, "
                + "description VARCHAR(100), "
                + "priority INTEGER, "
                + "completed BOOLEAN, "
                + "dueDate DATE, "
                + "alert BOOLEAN, "
                + "daysBefore INTEGER, "
                + "obs VARCHAR(250) "
                + ")");
    }

    public void disconnect() {
        try {
            if (con != null) {
                con.close();
            }
            con = null;
        } catch (SQLException e) {
            // ignores the exception
        }
    }

    private void cleanUp() {
        try {
            if (rs != null) {
                rs.close();
            }
            rs = null;
            if (stmt != null) {
                stmt.close();
            }
            stmt = null;
        } catch (SQLException e) {
            // ignores the exception
        }
    }

    private void update(String sql) throws DatabaseException {
        try {
            stmt = con.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot modify the database", e);
        } finally {
            cleanUp();
        }
    }

    private PreparedStatement prepare(String sql) throws SQLException {
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            stmt = pst;
            return pst;
        } finally {
            cleanUp();
        }
    }

    private List<Task> query(String where, String orderBy) throws DatabaseException {
        List<Task> result = new ArrayList<>();
        try {
            String sql = "SELECT id, description, priority, completed, "
                    + "dueDate, alert, daysBefore, obs FROM PUBLIC.todo ";
            if (where != null) {
                sql += "WHERE " + where + " ";
            }
            if (orderBy != null) {
                sql += "ORDER BY " + orderBy;
            }
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt(1));
                task.setDescription(rs.getString(2));
                task.setPriority(rs.getInt(3));
                task.setCompleted(rs.getBoolean(4));
                Date date = rs.getDate(5);
                task.setDueDate(date == null ? DATE_FORMATTER.format(Calendar.getInstance()) : DATE_FORMATTER.format(date));
                task.setAlert(rs.getBoolean(6));
                task.setDaysBefore(rs.getInt(7));
                task.setObs(rs.getString(8));
                result.add(task);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Cannot fetch from database", e);
        } finally {
            cleanUp();
        }
        return result;
    }

    private void modify(String sql, Task task) throws DatabaseException {
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            stmt = pst;
            pst.setString(1, task.getDescription());
            pst.setInt(2, task.getPriority());
            pst.setBoolean(3, task.isCompleted());
            if (task.getDueDate() == null) {
                pst.setDate(4, null);
            } else {
                pst.setDate(4, Date.valueOf(task.getDueDate()));
            }
            pst.setBoolean(5, task.isAlert());
            pst.setInt(6, task.getDaysBefore());
            pst.setString(7, task.getObs());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Cannot update the database", e);
        } finally {
            cleanUp();
        }
    }

    public List<Task> listAllTasks(boolean priorityOrDate) throws DatabaseException {
        return query(null, priorityOrDate
                ? "priority, dueDate, description" : "dueDate, priority, description");
    }

    public List<Task> listTasksWithAlert() throws ModelException {
        return query("alert = true AND "
                + "datediff('dd', CURRENT_TIMESTAMP, CAST(dueDate AS TIMESTAMP)) <= daysBefore",
                "dueDate, priority, description");
    }

    public void addTask(Task task) throws ValidationException, DatabaseException {
        validate(task);
        String sql = "INSERT INTO PUBLIC.todo ("
                + "description, priority, completed, dueDate, alert,"
                + "daysBefore, obs) VALUES (?, ?, ?, ?, ?, ?, ?)";
        modify(sql, task);
    }

    public void updateTask(Task task) throws ValidationException, DatabaseException {
        validate(task);
        String sql = "UPDATE PUBLIC.todo SET "
                + "description = ?, priority = ?, completed = ?, dueDate = ?, "
                + "alert = ?, daysBefore = ?, obs = ? "
                + "WHERE id = " + task.getId();
        modify(sql, task);
    }

    public void markAsCompleted(int id, boolean completed) throws DatabaseException {
        update("UPDATE PUBLIC.todo SET completed = " + completed + " "
                + "WHERE id = " + id);
    }

    public void removeTask(int id) throws DatabaseException {
        update("DELETE FROM PUBLIC.todo WHERE id = " + id);
    }

    private boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

    private void validate(final Task task) throws ValidationException {
        if (isEmpty(task.getDescription())) {
            throw new ValidationException("Must provide a task description");
        }
    }
}
