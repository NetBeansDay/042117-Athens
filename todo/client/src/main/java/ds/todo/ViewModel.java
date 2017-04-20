package ds.todo;

import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.Property;
import ds.todo.js.PlatformServices;
import net.java.html.json.ModelOperation;
import net.java.html.json.OnPropertyChange;
import ds.todo.js.PlatformServices;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ds.todo.persistence.DatabaseException;
import ds.todo.persistence.ValidationException;
import ds.todo.persistence.TaskManager;

@Model(className = "TaskList", targetId="", instance=true, properties = {
    @Property(name = "tasks", type = Task.class, array = true),
    @Property(name = "showCompleted", type = boolean.class),
    @Property(name = "sortByPriority", type = boolean.class),
    @Property(name = "dialog", type = boolean.class),
    @Property(name = "message", type = String.class),
    @Property(name = "selected", type = Task.class),
    @Property(name = "edited", type = Task.class)    
})
final class ViewModel {
    private PlatformServices services;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @ModelOperation
    void initServices(TaskList model, PlatformServices services) {
        this.services = services;
    }
    /**
     * Called when the page is ready.
     */
    static void onPageLoad(PlatformServices services) throws Exception {
        TaskList taskList = new TaskList();
        taskList.setSelected(null);
        taskList.setEdited(null);
        List<Task> tasks = TaskManager.getInstance().listAllTasks(true);
        for (Task task : tasks) {
            taskList.getTasks().add(task);
        }
        taskList.applyBindings();
    }

    @Function
    static void addNew(TaskList tasks) {
        tasks.setSelected(null);
        tasks.setEdited(new Task());
    }

    @Function
    static void edit(TaskList tasks, Task data) {
        tasks.setSelected(data);
        tasks.setEdited(data.clone());
    }    
    
    @Function
    public static void removeTask(TaskList tasks, Task data) {
        tasks.getTasks().remove(data);
        try {
            TaskManager.getInstance().removeTask(data.getId());
        } catch (DatabaseException ex) {
            Logger.getLogger(ViewModel.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }  
    
    @Function
    static void commit(TaskList tasks) {
        final Task task = tasks.getEdited();
        if (task == null || !validate(task)) {
            return;
        }
        final Task selectedTask = tasks.getSelected();
        if (selectedTask != null) {
            tasks.getTasks().set(tasks.getTasks().indexOf(selectedTask), task);
            try {
                TaskManager.getInstance().updateTask(task);
            } catch (DatabaseException | ValidationException ex) {
                Logger.getLogger(ViewModel.class.getName()).log(Level.SEVERE, null, ex);
            }            
        } else {
            tasks.getTasks().add(task);
            try {
                TaskManager.getInstance().addTask(task);
            } catch (DatabaseException | ValidationException ex) {
                Logger.getLogger(ViewModel.class.getName()).log(Level.SEVERE, null, ex);
            }              
        }
        tasks.setEdited(null);
    }    
    
    @Function
    static void cancel(TaskList tasks) {
        tasks.setSelected(null);
        tasks.setEdited(null);
    }    

    private static boolean validate(Task task) {
        String invalid = null;
        if (task.getValidate() != null) {
            invalid = task.getValidate();
        }
        return invalid == null;
    }    
    
    private static List<Task> listTasksWithAlert(List<Task> tasks) {
        List<Task> result = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            if (task.isAlert()) {
                result.add(task);
            }
        }        
        return result;
    }

    @ComputedProperty
    static int numberOfTasksWithAlert(List<Task> tasks) {
        return listTasksWithAlert(tasks).size();
    }    
    
    @Function
    static void expiredTasks(final TaskList tasks) {
        List<Task> listTasksWithAlert = listTasksWithAlert(tasks.getTasks());
        for (Task task : listTasksWithAlert) {
            showExpiredTask(tasks, task);
        }
    }

    private static void showExpiredTask(TaskList tasks, Task task) {
        tasks.setMessage("Task: " + task.getDescription() + "\nexpired on " + task.getDueDate());
        tasks.setDialog(true);     
    }    
    
    @Function
    public static void hideDialog(final TaskList tasks){
        tasks.setDialog(false);
    }    
    
    @ComputedProperty
    public static List<Task> sortedAndFilteredTasks(List<Task> tasks, boolean sortByPriority, boolean showCompleted) {
        List<Task> result = new ArrayList<>();
        if (showCompleted) {
            for (Task task : tasks) {
                if (task.isCompleted()) {
                    result.add(task);
                }
            }
        } else {
            result.addAll(tasks);
        }

        if (sortByPriority) {
            result.sort(new PriorityComparator());
        } else {
            result.sort(new DueDateComparator());            
        }
        return result;
    }    
    
    private static class PriorityComparator implements Comparator<Task> {

        @Override
        public int compare(final Task t1, final Task t2) {
            if (t1.getPriority() == t2.getPriority()) {
                return 0;
            } else if (t1.getPriority() > t2.getPriority()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private static class DueDateComparator implements Comparator<Task> {

        @Override
        public int compare(final Task t1, final Task t2) {
            try {
                Date t1DateDue = DATE_FORMATTER.parse(t1.getDueDate());
                Date t2DateDue = DATE_FORMATTER.parse(t2.getDueDate());
                return t1DateDue.compareTo(t2DateDue);
            } catch (ParseException ex) {
                Logger.getLogger(ViewModel.class.getName()).log(Level.WARNING, null, ex);
                return -1;
            }
        }
    }    
    
    @Model(className = "Task", targetId = "", properties = {
        @Property(name = "id", type = int.class),
        @Property(name = "description", type = String.class),
        @Property(name = "priority", type = int.class),
        @Property(name = "dueDate", type = String.class),
        @Property(name = "alert", type = boolean.class),
        @Property(name = "daysBefore", type = int.class),
        @Property(name = "obs", type = String.class),
        @Property(name = "completed", type = boolean.class)
    })
    public static class TaskModel {
        @ComputedProperty
        public static boolean isLate(String dueDate) {
            if (dueDate == null || dueDate.isEmpty()) {
                return false;
            }
            Date dateDue = null;
            try {
                dateDue = DATE_FORMATTER.parse(dueDate);
            } catch (ParseException ex) {
                Logger.getLogger(ViewModel.class.getName()).log(Level.WARNING, null, ex);
            }
            return (dateDue == null) ? false : dateDue.compareTo(Calendar.getInstance().getTime()) < 0;
        }

        @ComputedProperty
        static boolean hasAlert(String dueDate, boolean alert, int daysBefore) {
            if (dueDate == null || dueDate.isEmpty()) {
                return false;
            }
            Date dateDue = null;
            try {
                dateDue = DATE_FORMATTER.parse(dueDate);
            } catch (ParseException ex) {
                Logger.getLogger(ViewModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!alert || dateDue == null) {
                return false;
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateDue);
                int dias = cal.get(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                return dias <= daysBefore;
            }
        }
        
        @ComputedProperty
        static String validate(String description, int priority, String dueDate, int daysBefore) {
            String errorMsg = null;
            if (description == null || description.isEmpty()) {
                errorMsg = "Specify a description";
            }
            if (errorMsg == null && (priority < 1 || priority > 10)) {
                errorMsg = "Priority must be an integer in the range 1-10";
            }
            if (errorMsg == null) {
                if (dueDate == null) {
                    errorMsg = "Specify a valid due date";
                } else {
                    try {
                        Date dateDue = DATE_FORMATTER.parse(dueDate);
                        if (dateDue == null) {
                            errorMsg = "Specify a valid due date";
                        }
                    } catch (ParseException e) {
                        errorMsg = "Specify a valid due date";
                    }
                }
            }
            if (errorMsg == null && (daysBefore < 0 || daysBefore > 365)) {
                errorMsg = "Days before must be an integer in the range 0-365";
            }

            return errorMsg;
        }        
    }    
}
