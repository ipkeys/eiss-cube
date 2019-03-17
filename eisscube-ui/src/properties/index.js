import React from 'react';
import { 
    downloadCSV,
    List,
    Show,
    Edit,
    Create,
    Filter,
    Datagrid,
    TextField,
    SearchInput,
    TextInput,
    LongTextInput,
    ShowButton,
    Responsive, 
    SimpleList,
    SimpleForm,
    SimpleShowLayout,
    required,
    maxLength
} from 'react-admin';
import { unparse as convertToCSV } from 'papaparse/papaparse.min';
import Icon from '@material-ui/icons/LocalOffer';
import { withStyles } from '@material-ui/core/styles';

export const PropertyIcon = Icon;

const styles = theme => ({
    title: {
        color: theme.palette.common.white
    },
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    },
    showLayout: {
        paddingTop: theme.spacing.unit * 2
    }
});

const exporter = records => {
    const data = records.map(record => ({
        ...record
    }));

    const csv = convertToCSV({
        data,
        fields: ['name', 'label', 'description']
    });

    downloadCSV(csv, 'EISS™Cubes Properties');
};

const PropertyTitle = withStyles(styles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.name && `${record.name}`}
        </div>
    )
);

const PropertyListFilter = props => (
    <Filter {...props}>
        <SearchInput source='q' alwaysOn />
    </Filter>
);

export const PropertyList = withStyles(styles)(
    ({ classes, ...props }) => (
        <List  
            title={<PropertyTitle title='EISS™Cubes Property' />}
            filters={<PropertyListFilter />}
            sort={{ field: 'name', order: 'ASC' }}
            perPage={10}
            exporter={exporter}
            {...props}
        >
            <Responsive
                small={
                    <SimpleList
                        linkType="show"
                        primaryText={record => record.name}
                        secondaryText={record => record.label}
                    />
                }
                medium={
                    <Datagrid classes={{ rowEven: classes.rowEven }} >
                        <TextField source='name' label='Name' />
                        <TextField source='label' label='Label' />
                        <ShowButton />
                    </Datagrid>
                }
            />
        </List>
    )
);

export const PropertyShow = withStyles(styles)(
    ({ classes, ...props }) => (
        <Show 
            title={<PropertyTitle title='EISS™Cubes Property -' />}
            {...props}
        >
            <SimpleShowLayout className={classes.showLayout} >
                <TextField label='Name' source='name' />
                <TextField label='Label' source='label' />
                <TextField label='Description' source='description' />
            </SimpleShowLayout>
        </Show>
    )
);

const validateValue = [required(), maxLength(20)];

export const PropertyEdit = withStyles(styles)(
    ({ classes, ...props }) => (
        <Edit  
            title={<PropertyTitle title='Edit EISS™Cube Property -' />}
            {...props}
        >
            <SimpleForm redirect='show'>
                <TextInput label='Name' source='name' validate={validateValue} />
                <TextInput label='Label' source='label' validate={validateValue} />
                <LongTextInput label='Description' source='description' />
            </SimpleForm>
        </Edit>
    )
);

export const PropertyCreate = withStyles(styles)(
    ({ classes, ...props }) => (
        <Create 
            title={<PropertyTitle title='Create a new EISS™Cube Property' />} 
            {...props}
        >
            <SimpleForm redirect='show'>
                <TextInput label='Name' source='name' validate={validateValue} />
                <TextInput label='Label' source='label' validate={validateValue} />
                <LongTextInput label='Description' source='description' />
            </SimpleForm>
        </Create>
    )
);
