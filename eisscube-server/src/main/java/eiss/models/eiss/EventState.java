package eiss.models.eiss;

public enum EventState {

    CREATED {
        @Override
        public String toString() {
            return "EventCreated";
        }
    },
    MODIFIED {
        @Override
        public String toString() {
            return "EventModified";
        }
    },
    CANCELLED {
        @Override
        public String toString() {
            return "EventCancelled";
        }
    },
    PRE_START {
        @Override
        public String toString() {
            return "PreStart";
        }
    },
    START {
        @Override
        public String toString() {
            return "Start";
        }
    },
    ACTIVE {
        @Override
        public String toString() {
            return "EventActive";
        }
    },
    PRE_END {
        @Override
        public String toString() {
            return "PreEnd";
        }
    },
    END {
        @Override
        public String toString() {
            return "End";
        }
    },
    APPROVAL_PENDING {
        @Override
        public String toString() {
            return "Approval";
        }
    };

    public static EventState of(String name) {
        switch (name) {
            case "EventCreated":
                return EventState.CREATED;
            case "EventModified":
                return EventState.MODIFIED;
            case "EventCancelled":
                return EventState.CANCELLED;
            case "PreStart":
                return EventState.PRE_START;
            case "Start":
                return EventState.START;
            case "EventActive":
                return EventState.ACTIVE;
            case "PreEnd":
                return EventState.PRE_END;
            case "End":
                return EventState.END;
            case "Approval":
                return EventState.APPROVAL_PENDING;
            default:
                return EventState.APPROVAL_PENDING;
        }
    }

}
