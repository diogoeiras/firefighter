package events;

public class FiremanPersonEvent {

    // Fireman responsable for saving BDIAgent person
    private Object fireman_id;

    // Person to be saved by BDIAgent fireman
    private Object person_id;

    // Set a event as active or not
    private boolean status;

    // Set a event as complete or not
    private boolean completed;

    // Set to true if at least one object is dead
    private boolean deadEvent;

    public FiremanPersonEvent(Object fireman, Object person){
        this.fireman_id = fireman;
        this.person_id = person;
        this.status = true;
        this.completed = false;
        this.deadEvent = false;
    }

    public Object getFireman(){
        return fireman_id;
    }

    public Object getPerson(){
        return person_id;
    }

    public boolean getStatus() {
        return status;
    }

    public boolean getCompleted() {
        return completed;
    }

    public boolean getDeadEvent() {
        return deadEvent;
    }

    public void changeDeadEvent(){
        this.deadEvent = true;
    }

    public void changeStatus(){
        this.status = !this.status;
    }
}
