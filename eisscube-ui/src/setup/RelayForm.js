import React, { Component, Fragment } from 'react';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    TextInput,
    FormDataConsumer
} from 'react-admin';
import { FormSpy } from 'react-final-form';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
});

const RelaySettings = ({ formData }) => {
    return (
        formData && formData.relay && formData.relay.connected ?
        <Fragment>
            <SelectInput label='To contacts' source='relay.contacts' choices={[
                { id: 'NO', name: 'Normal Open' },
                { id: 'NC', name: 'Normal Close' }
            ]} fullWidth />
            <TextInput label='Label' source='relay.label' fullWidth />
            <TextInput label='Description' source='relay.description' fullWidth />
        </Fragment>
        : null
    );
};

class RelayForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };
    }
    
    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    render() {
        const { onSubmit, step, back, next } = this.props
        const { data } = this.state;
        
        return (
            <SimpleForm
                initialValues={data}
                save={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='relay.connected' />

                <FormDataConsumer>
                    {formDataProps => <RelaySettings {...formDataProps} /> }
                </FormDataConsumer>

                <FormSpy subscription={{ pristine: true, submitting: true }}>
                {props => (
                    <SetupFormButton step={step} onNext={next} onBack={back} pristine={props.pristine} submitting={props.submitting}/>                
                )}
                </FormSpy>
            </SimpleForm>
        );
    }

}

RelayForm.propTypes = {
    data: PropTypes.object
};

export default withStyles(styles)(RelayForm);
