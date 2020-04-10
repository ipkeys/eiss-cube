import React from 'react';
import {
	useShowController,
	List,
	Filter,
	Datagrid,
	ReferenceField,
	TextField,
	ShowButton,
	ReferenceInput,
	AutocompleteInput,
	Show,
	SimpleShowLayout,
} from 'react-admin';
import Typography from '@material-ui/core/Typography';
import { withStyles } from '@material-ui/core/styles';

import Icon from '@material-ui/icons/BarChart';

import PulseChart from './PulseChart';
import CycleChart from './CycleChart';

export const ReportIcon = Icon;

const styles = theme => ({
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    }
});

const ReportListTitle = ({title, record}) => (
	<Typography variant="h6">
		{title} { record && record.cubeName && `${record.cubeName}` }
	</Typography>
);

const ReportListFilter = props => (
	<Filter {...props}>
		<ReferenceInput alwaysOn label='from EISS™Cube' source='cubeID' reference='cubes'>
			<AutocompleteInput optionText='name'/>
		</ReferenceInput>
	</Filter>
);

export const ReportList = withStyles(styles)(
    ({ classes, permissions: p, ...props }) => (
		<List {...props}
			title={<ReportListTitle title='Reports' />}
			filters={<ReportListFilter permissions={p} />}
			sort={{ field: 'cubeID', order: 'ASC' }}
			perPage={10}
			exporter={false}
			bulkActionButtons={false}
		>
			<Datagrid classes={{ rowEven: classes.rowEven }} >
				<ReferenceField label='EISS™Cube' source='cubeID' reference='cubes' link='show'>
                    <TextField source='name' />
                </ReferenceField>
				<ShowButton />
			</Datagrid>
		</List>
    )
);
	

export const ReportShow = withStyles(styles)(
    ({ classes, permissions: p, ...props }) => {
		const { record } = useShowController(props);

		return (
			<Show {...props}
				title={<ReportListTitle title='Report from' />} 
			>
				<SimpleShowLayout>
					<PulseChart {...props} />
{/*}
					{record && record.type === 'p' &&
						<PulseChart {...props} />
					}
					{record && record.type === 'c' &&
						<CycleChart {...props} />
					}
*/}
					</SimpleShowLayout>
			</Show>
		);
	}
);
