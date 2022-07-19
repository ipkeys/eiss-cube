import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useLocation } from 'react-router-dom';
import {
	Box,
	Card,
	CardActions,
	CircularProgress
} from '@mui/material';
import { lightBlue } from '@mui/material/colors';
import {
	Form,
	Button,
	Notification,
	TextInput,
	useTranslate,
	useLogin,
	useNotify,
	useRedirect,
	required
} from 'react-admin';
import Logo from './Logo';

interface FormValues {
	username?: string;
	password?: string;
}

const LoginForm = () => {
	const [loading, setLoading] = useState(false);
	const translate = useTranslate();
	const notify = useNotify();
	const login = useLogin();
	const location = useLocation();
	const redirect = useRedirect();
	const form = useForm();

	const onSubmit = (auth: FormValues) => {
		setLoading(true);
		login(auth, location.state ? (location.state as any).nextPathname : '')
			.catch((error: any) => {
				if (error.status === 401 && error.statusText === 'LOCKED') {
					redirect('/locked');
				}
				else if (error.type  && error.code) {
					redirect(`/${error.type}/${error.code}`);
				}
				else if (error.mfa) {
					redirect('/mfa');
				}
				else {
					// other errors
					let msg: any = undefined;
					if (typeof error === 'undefined' || !error.message) {
						msg = translate('ra.auth.sign_in_error')
					} else if (typeof error === 'string') {
						msg = error;
					} else {
						msg = error.message;
					}

					notify(msg, {type: 'warning'});
				}
			})
			.finally(() => setLoading(false));
	};

	return (
		<Form onSubmit={onSubmit} noValidate>
			<Box sx={{
					display: 'flex',
					flexDirection: 'column',
					alignItems: 'center',
					justifyContent: 'flex-start'
				}}
			>
				<Card raised sx={{
						minWidth: 470,
						marginTop: '10em',
						backgroundColor: lightBlue[50]
					}}
				>

					<Logo />

					<Box sx={{ padding: '0 1em 1em 1em' }}>
						<TextInput
							autoFocus
							source='username'
							label={translate('ra.auth.username')}
							disabled={loading}
							validate={required()}
							fullWidth
						/>
						<TextInput
							source='password'
							label={translate('ra.auth.password')}
							type='password'
							disabled={loading}
							validate={required()}
							fullWidth
						/>
					</Box>
					<CardActions sx={{ padding: '0 7em 1em 7em' }}>
						<Button
							label={translate('ra.auth.sign_in')}
							variant='contained'
							type='submit'
							disabled={loading}
							fullWidth
						>
							{loading ? <CircularProgress size={25} thickness={2}/> : <></>}
						</Button>
					</CardActions>
				</Card>

				<Button sx={{ margin: '2em' }} label='forgot password?' onClick={() => {
					form.reset();
					redirect('/forgotpassword');
				}} />

				<Notification />
			</Box>
		</Form>
	);
};

export default LoginForm;
