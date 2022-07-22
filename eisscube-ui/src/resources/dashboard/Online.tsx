import { Avatar, Card, CardHeader } from '@mui/material';
import { ThumbUp, List } from '@mui/icons-material';
import { green } from '@mui/material/colors';
import { Button } from 'react-admin';

const Online = ({ value } : any) => (
	<Card raised sx={{
			borderLeft: `solid 4px ${green[500]}`, 
			flex: 1, 
			marginBottom: '1em' 
		}}
	>
		<CardHeader
			avatar={
				<Avatar sx={{
					color: theme => theme.palette.common.white,
					backgroundColor: green[500]
				}}
				>
					<ThumbUp />
				</Avatar>
			}
			title={`${value} EISS™Cube(s)`}
			subheader={<span style={{ color: green[500] }}>ONLINE</span>}
			action={
				<Button label='List' 
					href={'#/cubes?filter={"online":true}&page=1&perPage=10&sort=deviceID&order=DESC'} >
					<List />
				</Button>
			}
		/>
	</Card>
);

export default Online;
