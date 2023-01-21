import { Avatar, Card, CardHeader } from '@mui/material';
import { ThumbDown, List } from '@mui/icons-material';
import { red } from '@mui/material/colors';
import { Button } from 'react-admin';

const Offline = ({ value } : any) => (
	<Card raised sx={{
		borderLeft: `solid 4px ${red[500]}`,
		flex: 1,
		marginBottom: '1em'
	}}
	>
		<CardHeader
			avatar={
				<Avatar sx={{
					color: theme => theme.palette.common.white,
					backgroundColor: red[500]
				}}
				>
					<ThumbDown />
				</Avatar>
			}
			title={`${value} EISS™Cube(s)`}
			subheader={<span style={{ color: red[500] }}>OFFLINE</span>}
			action={
				<Button label='List'
					href={'#/cubes?filter={"online":false}&page=1&perPage=10&sort=deviceID&order=DESC'} >
					<List />
				</Button>
			}
		/>
	</Card>
);

export default Offline;
