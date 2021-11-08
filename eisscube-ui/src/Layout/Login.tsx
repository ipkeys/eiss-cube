import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Field, withTypes } from 'react-final-form';
import {
    Card,
    CardActions,
    CircularProgress,
    TextField,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { ThemeProvider } from '@material-ui/styles';
import { Button, Notification, useTranslate, useLogin, useNotify } from 'react-admin';
import { lightBlue } from '@material-ui/core/colors';
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
        minWidth: 460,
        marginTop: theme.spacing(20),
        backgroundColor: lightBlue[50]
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
    },
    link: {
        marginTop: theme.spacing(2),
        textDecoration: 'none'
    }
}));

const renderInput = ({
    meta: { touched, error } = { touched: false, error: undefined },
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

interface FormValues {
    username?: string;
    password?: string;
}

const { Form } = withTypes<FormValues>();

const LoginForm = () => {
    const [loading, setLoading] = useState(false);
    const translate = useTranslate();
    const classes = useStyles();
    const notify = useNotify();
    const login = useLogin();
    const location = useLocation<{ nextPathname: string } | null>();

    const onSubmit = (auth: FormValues) => {
        setLoading(true);
        login(auth, location.state ? location.state.nextPathname : '/')
            .catch((error: Error) => {
                setLoading(false);
                
                let msg: any = undefined;
                if (typeof error === 'undefined' || !error.message) {
                    msg = translate('ra.auth.sign_in_error')
                } else if (typeof error === 'string') {
                    msg = error;
                } else {
                    msg = error.message;
                }

                notify(msg, 'warning');
            }
        );
    };

    const validate = (values: FormValues) => {
        const errors: FormValues = {};

        if (!values.username) {
            errors.username = translate('ra.validation.required');
        }

        if (!values.password) {
            errors.password = translate('ra.validation.required');
        }

        return errors;
    };

    const progress = loading ?
		<CircularProgress
			className={classes.loginProgress}
			size={25}
			thickness={2}
		/>
	: <></>;

    return (
        <Form
            onSubmit={onSubmit}
            validate={validate}
            render={({ handleSubmit, form }) => (
                <div className={classes.main}>
                    <form onSubmit={handleSubmit} noValidate>
                        <Card className={classes.card}>
                            
                            <Logo />
                            
                            <div className={classes.form}>
                                <div className={classes.input}>
                                    <Field
                                        autoFocus
                                        name="username"
                                        // @ts-ignore
                                        component={renderInput}
                                        label={translate('ra.auth.username')}
                                        disabled={loading}
                                        inputProps={{ autoComplete: 'off' }}
                                    />
                                </div>
                                <div className={classes.input}>
                                    <Field
                                        name="password"
                                        // @ts-ignore
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
                                    label={translate('ra.auth.sign_in')}
                                    variant='contained'
                                    type='submit'
                                    disabled={loading}
                                    fullWidth
                                >
                                    {progress}
                                </Button>
                            </CardActions>
                        </Card>
                    </form>

                    <Link className={classes.link} to='/forgotpassword' onClick={() => form.reset()}>
                        <Button label='forgot password?' />
                    </Link>

                    <Notification />
                </div>
            )}
        />
    );
};

// We need to put the ThemeProvider decoration in another component
// Because otherwise the useStyles() hook used in Login won't get
// the right theme
const LoginFormWithTheme = (props: any) => (
    <ThemeProvider theme={props.theme}>
        <LoginForm {...props} />
    </ThemeProvider>
);

export default LoginFormWithTheme;
