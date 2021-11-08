import { isAdmin, isSuper } from './Roles';

export const canEdit = (record: any, p: any) => {
    if (!p) return false;
    if (isSuper(p)) return true;
    if (record && record.id) {
        // Can edit self
        if (record.id === p.user_id) {
            return true;
        }
        // Admin can edit other non-admins
        else if (isAdmin(p) && record.role) {
            const permissions = {role: record.role};
            if (isSuper(permissions) || isAdmin(permissions)) return false;
            else return true;
        }
    }
    return false;
}

export const canDelete = (record: any, p: any) => {
    if (!p) return false;
    if (record && record.id) {
        // sys admin can't delete self
        if (isSuper(p)) {
          if (record.id !== p.user_id) return true;
          else return false;
        }
        // Admin can't delete admins
        if (isAdmin(p) && record.role) {
            const permissions = {role: record.role};
            if (isSuper(permissions) || isAdmin(permissions)) return false;
            else return true;
        }
    }
    return false;
}

export const canDeleteGroup = (record: any, p: any) => {
    if (!p) return false;
    if (record && record.id) {
        if (isSuper(p)) {
            if (record.id !== p.user_id && record.numUsers === 0) return true;
            else return false;
        }
    }
    return false;
}

export const canDoMFA = (record: any, p: any) => {
    if (!p) return false;
    if (record && record.id && record.id === p.user_id) {
        return true;
    }
    return false;
}
