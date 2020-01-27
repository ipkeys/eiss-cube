import React, { Component, Fragment } from 'react';
import { withStyles } from '@material-ui/core/styles';
import FormGroup from '@material-ui/core/FormGroup';
import { GET_LIST, TextInput, maxLength } from 'react-admin';
import { dataProvider } from '../providers';

const styles = theme => ({
    longText: {
        minWidth: theme.spacing(66) 
    },
    formGroup: {
        marginTop: theme.spacing(1)
    }
});

class Settings extends Component {

    state = {
        properties: []
    };

	componentWillMount() {
		dataProvider(GET_LIST, 'properties', {
            sort: { field: 'name', order: 'ASC' },
            pagination: { page: 1, perPage: 100 }
        })
		.then(response => response.data)
		.then(data => {
			this.setState({
				properties: data
			});
		});
    }
        
    render() {
        const { classes } = this.props;
        const { properties } = this.state;

        return (
            <Fragment>
                {properties && properties.map(property => (
                    <FormGroup key={property.id} row>
                        <TextInput
                            className={classes.longText} 
                            source={`settings.${property.name}`}
                            label={property.label} 
                            helperText={property.description}
                            margin='normal'
                            validate={[maxLength(50)]}
                        />
                    </FormGroup>
                ))}
            </Fragment>
        );
    }
};

export default withStyles(styles)(Settings);
