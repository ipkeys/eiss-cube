import { useState, useEffect } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';
import { Title, Loading, Error, useGetList } from 'react-admin';
import Welcome from './Welcome';
import Online from './Online';
import Offline from './Offline';
import CubesMap from './CubesMap';

const useStyles = makeStyles((theme: Theme) => ({ 
    root: {
        paddingTop: theme.spacing(2),
        flexGrow: 1
    }
}));

const Dashboard = () =>  {
    const classes = useStyles();
    const [online, setOnline] = useState<number>(0);
    const [offline, setOffline] = useState<number>(0);
    const { data, ids, loading, error } = useGetList(
        'cubes', 
        { page: 1, perPage: 100 },
        { field: 'name', order: 'ASC' },
        { }
    );

    useEffect(() => {
        const values = ids && ids.reduce((stats, id) => {
            data[id].online ? stats.on++ : stats.off++;
            return stats;
        }, {on: 0, off: 0});

        setOnline(values.on);
        setOffline(values.off);
	}, [data, ids]);

    if (loading) return <Loading />;
    if (error) return <Error error={error} />;

    return (
        <div className={classes.root} >
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
                    <CubesMap data={data} ids={ids} />
                </Grid>
            </Grid>
        </div>
    );
}

export default Dashboard;
