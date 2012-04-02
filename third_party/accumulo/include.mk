# Copyright (C) 2011-2012  The OpenTSDB Authors.
#
# This library is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published
# by the Free Software Foundation, either version 2.1 of the License, or
# (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this library.  If not, see <http://www.gnu.org/licenses/>.

ACCUMULO_VERSION := 1.4.0
ACCUMULO := third_party/accumulo/accumulo-core-$(ACCUMULO_VERSION).jar
ACCUMULO_BASE_URL := https://repository.apache.org/content/groups/public/org/apache/accumulo/accumulo-core/1.4.0

$(ACCUMULO): $(ACCUMULO).md5
	set dummy "$(ACCUMULO_BASE_URL)" "$(ACCUMULO)"; shift; $(FETCH_DEPENDENCY)

THIRD_PARTY += $(ACCUMULO)
