// Defined maping to database user roles
// If they change in the database, we won't have to change them across the ui

export const SYSADMIN = "securityadmin";
export const ADMIN = "admin";
export const MANAGER = "manager";
export const OPERATOR = "operator";
export const VIEWER = "viewer";
export const VEN = "ven";

export const roles = [
    { name: "Security Administrator", id: SYSADMIN},
    { name: "Administrator", id: ADMIN},
    { name: "Manager", id: MANAGER},
    { name: "Operator", id: OPERATOR},
    { name: "Viewer", id: VIEWER},
    { name: "VEN", id: VEN}
];

export const less_roles = [
    { name: "Administrator", id: ADMIN},
    { name: "Manager", id: MANAGER},
    { name: "Operator", id: OPERATOR},
    { name: "Viewer", id: VIEWER},
    { name: "VEN", id: VEN}
];

export const isSuper = (permissions: any) => (
    permissions && permissions.role === SYSADMIN
);

export const notSuper = (permissions: any) => (
    permissions && permissions.role !== SYSADMIN
);

export const isAdmin = (permissions: any) => (
    permissions && permissions.role === ADMIN
);

export const notAdmin = (permissions: any) => (
    permissions && permissions.role !== ADMIN
);
