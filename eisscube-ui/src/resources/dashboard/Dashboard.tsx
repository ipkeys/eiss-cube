import { useState, useEffect } from 'react';
import { Box, Grid } from '@mui/material';
import { Title, Loading, useGetList } from 'react-admin';
import Welcome from './Welcome';
import Online from './Online';
import Offline from './Offline';
import CubesMap from './CubesMap';

const Dashboard = () =>  {
	const [online, setOnline] = useState<number>(0);
	const [offline, setOffline] = useState<number>(0);
	const { data, isLoading } = useGetList('cubes',
		{
			pagination: { page: 1, perPage: 100 },
			sort: { field: 'name', order: 'ASC' }  
		} 
	);

	useEffect(() => {
		const values = data && data.reduce((stats, cube) => {
			cube.online ? stats.on++ : stats.off++;
			return stats;
		}, {on: 0, off: 0});

		if (values) {
			setOnline(values.on);
			setOffline(values.off);
		}
	}, [data]);

	if (isLoading) return <Loading />;

	return (
		<Box sx={{
				paddingTop: '2em',
				flexGrow: 1    
			}}
		>
			<Title title="EISS™Cube Server" />
			<Grid container spacing={2}>
				<Grid item xs={12}>
					<Welcome />
				</Grid>
				<Grid item xs={6}>
					<Online value={online} />
				</Grid>
				<Grid item xs={6}>
					<Offline value={offline} />
				</Grid>
				<Grid item xs={12}>
					<CubesMap data={data} />
				</Grid>
			</Grid>
		</Box>
	);
}

export default Dashboard;
