import React, { useState } from 'react';
import { Form, Field } from 'react-final-form';

import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import CircularProgress from '@material-ui/core/CircularProgress';
import { makeStyles } from '@material-ui/core/styles';
import { ThemeProvider } from '@material-ui/styles';
import { lightBlue } from '@material-ui/core/colors';

import { Notification } from 'react-admin';
import { useTranslate, useLogin, useNotify } from 'ra-core';
import Logo from './Logo';

const useStyles = makeStyles(theme => ({
    main: {
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'flex-start'
    },
    card: {
        minWidth: 440,
        marginTop: theme.spacing(20),
        backgroundColor: lightBlue[50]
    },
    icon: {
        marginTop: theme.spacing(1),
        backgroundColor: theme.palette.secondary.light
    },
    form: {
        padding: theme.spacing(2)
    },
    input: {
        marginTop: theme.spacing(1)
    },
    actions: {
        padding: theme.spacing(2)
    },
    loginProgress: {
        marginRight: theme.spacing(2)
    }
}));

const renderInput = ({
    meta: { touched, error } = { },
    input: { ...inputProps },
    ...props
}) => (
    <TextField
        error={!!(touched && error)}
        helperText={touched && error}
        {...inputProps}
        {...props}
        fullWidth
    />
);


const Login = ({ location }) => {
    const [loading, setLoading] = useState(false);
    const translate = useTranslate();
    const classes = useStyles();
    const notify = useNotify();
    const login = useLogin();

    const handleSubmit = (auth) => {
        setLoading(true);
        login(auth, location.state ? location.state.nextPathname : '/')
            .catch((error) => {
                setLoading(false);
                notify(
                    typeof error === 'string'
                        ? error
                        : typeof error === 'undefined' || !error.message
                        ? 'ra.auth.sign_in_error'
                        : error.message,
                    'warning'
                );
            }
        );
    };

    const validate = (values) => {
        const errors = {};
        if (!values.username) {
            errors.username = translate('ra.validation.required');
        }
        if (!values.password) {
            errors.password = translate('ra.validation.required');
        }
        return errors;
    };

    return (
        <Form
            onSubmit={handleSubmit}
            validate={validate}
            render={({ handleSubmit }) => (
                <form onSubmit={handleSubmit} noValidate>
                    <div className={classes.main}>
                        <Card className={classes.card}>
                            <Logo />
                            <div className={classes.form}>
                                <div className={classes.input}>
                                    <Field
                                        autoFocus
                                        name="username"
                                        component={renderInput}
                                        label={translate('ra.auth.username')}
                                        disabled={loading}
                                        inputProps={{ autoComplete: 'off' }}
                                    />
                                </div>
                                <div className={classes.input}>
                                    <Field
                                        name="password"
                                        component={renderInput}
                                        label={translate('ra.auth.password')}
                                        type="password"
                                        disabled={loading}
                                        inputProps={{ autoComplete: 'off' }}
                                    />
                                </div>
                            </div>
                            <CardActions className={classes.actions}>
                                <Button
                                    variant="contained"
                                    type="submit"
                                    color="primary"
                                    disabled={loading}
                                    fullWidth
                                >
                                    {loading && (
                                        <CircularProgress
                                            className={classes.loginProgress}
                                            size={25}
                                            thickness={2}
                                        />
                                    )}
                                    {translate('ra.auth.sign_in')}
                                </Button>
                            </CardActions>
                        </Card>
                        <Notification />
                    </div>
                </form>
            )}
        />
    );
};

const LoginWithTheme = (props) => (
    <ThemeProvider theme={props.theme}>
        <Login {...props} />
    </ThemeProvider>
);

export default LoginWithTheme;
