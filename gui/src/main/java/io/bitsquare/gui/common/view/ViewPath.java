/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.common.view;

import io.bitsquare.app.Version;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ViewPath extends ArrayList<Class<? extends View>> implements Serializable {
    // That object is saved to disc. We need to take care of changes to not break deserialization.
    private static final long serialVersionUID = Version.LOCAL_DB_VERSION;

    public ViewPath() {
    }

    public ViewPath(Collection<? extends Class<? extends View>> c) {
        super(c);
    }

    public static ViewPath to(Class<? extends View>... elements) {
        ViewPath path = new ViewPath();
        List<Class<? extends View>> list = Arrays.asList(elements);
        path.addAll(list);
        return path;
    }

    public static ViewPath from(ViewPath original) {
        ViewPath path = new ViewPath();
        path.addAll(original);
        return path;
    }

    public Class<? extends View> tip() {
        if (size() == 0)
            return null;

        return get(size() - 1);
    }
}
