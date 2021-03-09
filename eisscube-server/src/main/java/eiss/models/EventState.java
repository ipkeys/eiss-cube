package eiss.models;

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
        return switch (name) {
            case "EventCreated" -> EventState.CREATED;
            case "EventModified" -> EventState.MODIFIED;
            case "EventCancelled" -> EventState.CANCELLED;
            case "PreStart" -> EventState.PRE_START;
            case "Start" -> EventState.START;
            case "EventActive" -> EventState.ACTIVE;
            case "PreEnd" -> EventState.PRE_END;
            case "End" -> EventState.END;
            default -> EventState.APPROVAL_PENDING;
        };
    }

}
