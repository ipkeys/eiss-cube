import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import Paper from '@material-ui/core/Paper';

import { Title, GET_LIST } from 'react-admin';
import { dataProvider } from '../App';

import Welcome from './Welcome';
import Online from './Online';
import Offline from './Offline';
import CubesMap from './CubesMap';

const styles = {
    root: {
        flexGrow: 1,
    },
    paperMap: { 
        height: '100%' 
    }
};

class Dashboard extends Component {
    state = {};

    componentWillMount() {
        dataProvider(GET_LIST, 'cubes', {
            sort: { field: 'deviceID', order: 'ASC' },
            pagination: { page: 1, perPage: 100 }
        })
        .then(response => response.data
            .reduce((stats, cube) => {
                cube.online ? stats.online++ : stats.offline++;
                return stats;
            }, { online: 0, offline: 0 })
        )
        .then(({ online, offline }) => {
            this.setState({ online, offline });
        });
    }

    render() {
        const { classes } = this.props;
        const { online, offline } = this.state;

        return (
            <div className={classes.root}>
                <Title title = "EISS™Cube Server" />
                <Grid container spacing={16}>
                    <Grid item xs={12}>
                        <Welcome />
                    </Grid>
                    <Grid item xs={6}>
                        <Online value = { online } />
                    </Grid>
                    <Grid item xs={6}>
                        <Offline value = { offline } />
                    </Grid>
                    <Grid item xs={12}>
                        <Paper elevation={1}>
                            <CubesMap />
                        </Paper>
                    </Grid>
                </Grid>
            </div>
        );
    }
}

export default withStyles(styles)(Dashboard);
