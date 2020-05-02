import React, { Component } from 'react';
import Typography from '@material-ui/core/Typography';
import { withStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';

import { Title, GET_LIST } from 'react-admin';
import { dataProvider } from '../providers';

import Welcome from './Welcome';
import Online from './Online';
import Offline from './Offline';
import CubesMap from './CubesMap';

const styles = theme => ({
    root: {
        paddingTop: theme.spacing(2),
        flexGrow: 1
    },
    title: {
        color: theme.palette.common.white
    },
});

const DashboardTitle = withStyles(styles)(
    ({classes, title}) => (
        <Typography className={classes.title} variant="h6">
            {title}
        </Typography>
    )
);

class Dashboard extends Component {
    state = {
        online: 0,
        offline: 0
    };

    componentDidMount() {
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
            <div className={classes.root} >
                <Title title={<DashboardTitle title="EISS™Cube Server"/>} />
                <Grid container spacing={2}>
                    <Grid item xs={12}>
                        <Welcome />
                    </Grid>
                    <Grid item xs={6}>
                        <Online value={ online } />
                    </Grid>
                    <Grid item xs={6}>
                        <Offline value={ offline } />
                    </Grid>
                    <Grid item xs={12}>
                        <CubesMap />
                    </Grid>
                </Grid>
            </div>
        );
    }
}

export default withStyles(styles)(Dashboard);
