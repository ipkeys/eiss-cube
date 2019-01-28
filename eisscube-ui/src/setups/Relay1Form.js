import React, { Component, Fragment } from 'react';
import compose from 'recompose/compose';
import { connect } from 'react-redux';
import { reset, submit, isPristine, isSubmitting, formValueSelector } from 'redux-form';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    NumberInput,
    TextInput
} from 'react-admin';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
    inline: { 
        display: 'inline-block',
        marginRight: theme.spacing.unit * 2, 
        minWidth: theme.spacing.unit * 32   
    }
});

class Relay1Form extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };

        this.handleSave = this.handleSave.bind(this);
    }
    
    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    handleSave = () => {
        const { submit } = this.props;
        submit('SetupCubeWizard');
    };

    render() {
        const { classes, onSubmit, isPristine, isSubmitting, step, back, next, isRelay1Connected } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                form='SetupCubeWizard'
                defaultValue={data}
                onSubmit={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='relay1.connected' className={classes.inline} margin='dense'/>
                {isRelay1Connected &&
                    <Fragment>
                        <SelectInput label='To contacts' source="relay1.contacts" choices={[
                            { id: 'NO', name: 'Normal Open' },
                            { id: 'NC', name: 'Normal Close' }
                        ]} className={classes.inline} margin='dense' />
                        <NumberInput label='Load value (W)' source='relay1.load' className={classes.inline} margin='dense' />
                        <TextInput label='Label' source='relay1.label' fullWidth margin='dense'/>
                        <TextInput label='Description' source='relay1.description' fullWidth margin='dense'/>
                    </Fragment>
                }
                <SetupFormButton step={step} onSave={this.handleSave} onNext={next} onBack={back} pristine={isPristine} submitting={isSubmitting}/>
            </SimpleForm>
        );
    }

}

Relay1Form.propTypes = {
    data: PropTypes.object
};

const selector = formValueSelector('SetupCubeWizard');

const mapStateToProps = state => ({
	isRelay1Connected: selector(state, 'relay1.connected'),
    isSubmitting: isSubmitting('SetupCubeWizard')(state),
    isPristine: isPristine('SetupCubeWizard')(state)
});

const mapDispatchToProps = {
	reset,
    submit
};

const enhance = compose(
    withStyles(styles)
);

export default enhance(
	connect(mapStateToProps, mapDispatchToProps)(
    	Relay1Form
	)
);
