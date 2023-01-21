import { Avatar, Card, CardHeader } from '@mui/material/';
import LightBulbIcon from '@mui/icons-material/EmojiObjectsOutlined';

const Welcome = () => (
	<Card raised sx={{
		borderLeft: theme => `solid 4px ${theme.palette.primary.main}`,
		flex: 1,
		marginBottom: '1em'
	}}
	>
		<CardHeader
			avatar={
				<Avatar sx={{
					color: theme => theme.palette.common.white,
					backgroundColor: theme => theme.palette.primary.main
				}}>
					<LightBulbIcon />
				</Avatar>
			}
			title='Welcome to the EISS™Cube Server'
			subheader='Handling EISS™Cubes, sending commands, collecting reports...'
		/>
	</Card>
);

export default Welcome;
