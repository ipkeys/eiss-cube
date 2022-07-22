import { Box } from '@mui/material';
import { logoUrl } from '../global';

const Logo = () => (
	<Box sx={{
		background: `url(${logoUrl})`,
		backgroundRepeat: 'no-repeat',
		backgroundPosition: 'center',
		marginTop: '1em',
		minHeight: '5em'
	}} />
);

export default Logo;
